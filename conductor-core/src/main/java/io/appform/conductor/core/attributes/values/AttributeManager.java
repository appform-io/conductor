/*
 * Copyright (c) 2024 santanu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.attributes.values;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.attributes.definition.impl.ChoiceAttributeDefinition;
import io.appform.conductor.model.attributes.definition.impl.NumberAttributeDefinition;
import io.appform.conductor.model.attributes.definition.impl.StringAttributeDefinition;
import io.appform.conductor.model.attributes.value.AttributeValue;
import io.appform.conductor.model.attributes.value.AttributeValueVisitor;
import io.appform.conductor.model.attributes.value.impl.*;
import io.appform.conductor.server.attributes.definition.AttributeDefinitionStore;
import io.appform.conductor.server.utils.Pair;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.appform.conductor.server.attributes.values.AttributeManager.AttributeValidationStatus.AttributeValidationResult.failure;
import static io.appform.conductor.server.attributes.values.AttributeManager.AttributeValidationStatus.AttributeValidationResult.success;

/**
 *
 */
@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AttributeManager {
    private final AttributeDefinitionStore definitionStore;
    private final AttributeValueStore valueStore;

    @Value
    public static class MaterializedAttributeValue {
        AttributeDefinition definition;
        AttributeValue value;
    }

    @Value
    public static class AttributeValidationStatus {
        public enum Status {
            SUCCESS,
            FAILURE
        }

        @Value
        public static class AttributeValidationResult {
            Status status;
            String message;
            @Nullable
            MaterializedAttributeValue savedAttribute;

            public static AttributeValidationResult success(final MaterializedAttributeValue savedAttribute) {
                return new AttributeValidationResult(Status.SUCCESS, "Success", savedAttribute);
            }

            public static AttributeValidationResult failure(final String failureMessage) {
                return new AttributeValidationResult(Status.FAILURE, failureMessage, null);
            }
        }

        Map<String, AttributeValidationResult> validationResults;
    }

    public AttributeValidationStatus save(
            final AttributeScopeType scopeType,
            final String objectRefId,
            final MultivaluedMap<String, String> form) {
        val definitions = definitionStore.readAll(scopeType)
                .stream()
                .collect(Collectors.toUnmodifiableMap(AttributeDefinition::getId, Function.identity()));
        val attributes = form.entrySet()
                .stream()
                .filter(entry -> entry.getValue().stream().noneMatch(String::isEmpty))
                .map(entry -> {
                    val def = definitions.get(entry.getKey());
                    Preconditions.checkNotNull(def, "No definition found for attribute: " + entry.getKey());
                    return switch (def.getType()) {
                        case STRING -> new StringAttributeValue(entry.getKey(), entry.getValue().get(0));
                        case CHOICE -> new ChoiceAttributeValue(entry.getKey(), entry.getValue());
                        case NUMBER -> new NumberAttributeValue(entry.getKey(),
                                                                Double.parseDouble(entry.getValue().get(0)));
                        case DATE -> new DateAttributeValue(entry.getKey(), parseHtmlDate(entry));
                        case LINK -> new LinkAttributeValue(entry.getKey(),
                                                            entry.getValue().get(0),
                                                            entry.getValue().get(0));
                    };
                })
                .toList();
        return save(scopeType, objectRefId, attributes, definitions);
    }

    public AttributeValidationStatus save(
            final AttributeScopeType scopeType,
            final String objectRefId,
            final List<AttributeValue> attributes) {
        return save(scopeType, objectRefId, attributes,
                    definitionStore.readAll(scopeType)
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(AttributeDefinition::getId, Function.identity())));
    }

    public AttributeValidationStatus save(
            final AttributeScopeType scopeType,
            final String objectRefId,
            final List<AttributeValue> attributes,
            final Map<String, AttributeDefinition> definitions) {
        val validationStatus = validateAttributes(attributes, definitions);
        val validAttributes = validationStatus.getValidationResults()
                .values()
                .stream()
                .filter(attributeValidationResult -> attributeValidationResult.getSavedAttribute() != null)
                .filter(attributeValidationResult -> attributeValidationResult.getStatus()
                        .equals(AttributeValidationStatus.Status.SUCCESS))
                .map(attributeValidationResult -> attributeValidationResult.getSavedAttribute().getValue())
                .toList();
        if (!validAttributes.isEmpty()) {
            valueStore.save(scopeType, objectRefId, validAttributes);
            log.debug("Saved attributes for {}/{}: {}",
                      scopeType,
                      objectRefId,
                      validAttributes.stream().map(AttributeValue::getSchemaId).toList());
        }
        return validationStatus;
    }

    public List<MaterializedAttributeValue> read(
            final AttributeScopeType scopeType,
            final String objRefId) {
        val values = valueStore.read(scopeType, objRefId)
                .stream()
                .collect(Collectors.toUnmodifiableMap(AttributeValue::getSchemaId, Function.identity()));
        return definitionStore.readAll(scopeType)
                .stream()
                .map(definition -> new MaterializedAttributeValue(definition, values.get(definition.getId())))
                .toList();
    }


    @SneakyThrows
    private static Date parseHtmlDate(Map.Entry<String, List<String>> entry) {
        return new SimpleDateFormat("yyyy-MM-dd").parse(entry.getValue().get(0));
    }

    private static AttributeValidationStatus validateAttributes(
            List<AttributeValue> attributes,
            Map<String, AttributeDefinition> definitions) {
        return new AttributeValidationStatus(
                attributes.stream()
                        .map(attributeValue -> {
                            val defintion = definitions.get(attributeValue.getSchemaId());
                            if (null == defintion) {
                                return Pair.of(attributeValue.getSchemaId(),
                                               failure("No attribute definition found"));
                            }
                            if (defintion.getType() != attributeValue.getType()) {
                                return Pair.of(attributeValue.getSchemaId(),
                                               failure("Attribute type mismatch. Required: " + defintion.getType()
                                                               + " received: " + attributeValue.getType()
                                                               + " for attribute: " + attributeValue.getSchemaId()));
                            }
                            val errors = new ArrayList<String>();
                            attributeValue.accept(new AttributeValueVisitor<>() {
                                @Override
                                public Pair<String,
                                        AttributeValidationStatus.AttributeValidationResult> visit(
                                        StringAttributeValue stringAttributeValue) {
                                    val def = (StringAttributeDefinition) defintion;
                                    if (def.getMaxLength() > 0 && stringAttributeValue.getValue()
                                            .length() > def.getMaxLength()) {
                                        errors.add(String.format("Attribute %s length exceeds maximim allowed %d",
                                                                 stringAttributeValue.getSchemaId(),
                                                                 def.getMaxLength()));
                                    }
                                    if (!Strings.isNullOrEmpty(def.getPattern())
                                            && !Pattern.matches(def.getPattern(), stringAttributeValue.getValue())) {
                                        errors.add(String.format(
                                                "Attribute %s doesn't match the required pattern. Expected: %s",
                                                stringAttributeValue.getSchemaId(),
                                                def.getPattern()));
                                    }
                                    return null;
                                }

                                @Override
                                public Pair<String,
                                        AttributeValidationStatus.AttributeValidationResult> visit(
                                        NumberAttributeValue numberAttributeValue) {
                                    val def = (NumberAttributeDefinition) defintion;
                                    val num = numberAttributeValue.getValue();
                                    if (num < def.getMin() || num > def.getMax()) {
                                        errors.add(String.format("Attribute %s is not in range: [ %f , %f ]",
                                                                 numberAttributeValue.getSchemaId(),
                                                                 def.getMin(),
                                                                 def.getMax()));
                                    }
                                    return null;
                                }

                                @Override
                                public Pair<String,
                                        AttributeValidationStatus.AttributeValidationResult> visit(
                                        ChoiceAttributeValue choiceAttributeValue) {
                                    val def = (ChoiceAttributeDefinition) defintion;
                                    if (!def.getOptions().containsAll(choiceAttributeValue.getValue())) {
                                        errors.add(String.format(
                                                "Attribute %s selected value(s) [%s] is not in allowed options: [%s]",
                                                choiceAttributeValue.getSchemaId(),
                                                Joiner.on(",").join(choiceAttributeValue.getValue()),
                                                Joiner.on(",").join(def.getOptions())));
                                    }
                                    return null;
                                }

                                @Override
                                public Pair<String,
                                        AttributeValidationStatus.AttributeValidationResult> visit(
                                        DateAttributeValue dateAttributeValue) {
                                    return null;
                                }

                                @Override
                                public Pair<String,
                                        AttributeValidationStatus.AttributeValidationResult> visit(
                                        LinkAttributeValue linkAttributeValue) {
                                    try {
                                        new URL(linkAttributeValue.getValue());
                                    }
                                    catch (MalformedURLException e) {
                                        errors.add(String.format(
                                                "Attribute %s requires a valid url. PArsing error: %s",
                                                linkAttributeValue.getSchemaId(), e.getMessage()));
                                    }
                                    return null;
                                }
                            });
                            if (errors.isEmpty()) {
                                return Pair.of(attributeValue.getSchemaId(),
                                               success(new MaterializedAttributeValue(defintion, attributeValue)));
                            }
                            return Pair.of(attributeValue.getSchemaId(),
                                           failure("Validation errors: " + Joiner.on(",").join(errors)));
                        })
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
    }
}
