/*
 * Copyright (c) 2023 Santanu Sinha
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

package io.appform.conductor.server.ticketmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldSchemaVisitor;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.model.ticket.fields.FieldValue;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.server.schemamanagement.SchemaOpValidationResult;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class TicketFieldMapper {

    public SchemaOpValidationResult<List<TicketFieldData>> map(final Schema schema, final JsonNode data) {
        val fields = data.fields();
        val schemaId = schema.getId();
        val fieldsMap = schema.getFields()
                .stream()
                .collect(Collectors.toMap(FieldSchema::getId, Function.identity()));
        val errors = new ArrayList<String>();
        val ticketFields = new ArrayList<TicketFieldData>();
        for (Iterator<Map.Entry<String, JsonNode>> it = fields; it.hasNext(); ) {
            val field = it.next();
            val fieldData = field.getValue();
            val fieldName = field.getKey();
            val fieldId = ConductorServerUtils.readableId(schemaId, fieldName);
            val fieldSchema = fieldsMap.get(fieldId);
            if(null == fieldSchema) {
                log.info("Skipping field mapping as no field schema found for field: " + fieldId);
                continue;
            }
            if (null == fieldData) {
                errors.add("No field found for field: " + field);
                continue;
            }
            val results = validateField(fieldSchema, fieldName, fieldData);
            if (!results.getFirst().isEmpty()) {
                errors.addAll(results.getFirst());
            }
            else {
                ticketFields.add(new TicketFieldData(fieldId, results.getSecond()));
            }
        }
        if (!errors.isEmpty()) {
            return SchemaOpValidationResult.failure(errors);
        }
        return SchemaOpValidationResult.success(ticketFields);
    }


    public Pair<List<String>, FieldValue> validateField(
            final FieldSchema schema,
            final String fieldName,
            final JsonNode fieldData) {
        val errors = new ArrayList<String>();
        return schema.accept(new FieldSchemaVisitor<>() {
            @Override
            public Pair<List<String>, FieldValue> visit(StringFieldSchema stringField) {
                if (!fieldData.isTextual()) {
                    return new Pair<>(List.of("Field " + fieldName + " is not text"), null);
                }
                val value = fieldData.asText();
                if (stringField.getMaxLength() != 0 && value.length() > stringField.getMaxLength()) {
                    errors.add("Field " + fieldName + " exceeds maximum field length of " + stringField.getMaxLength());
                }
                if (!Strings.isNullOrEmpty(stringField.getMatchPattern())
                        && !value.matches(stringField.getMatchPattern())) {
                    errors.add("Field " + fieldName + " does not match provided regex pattern");
                }
                return errors.isEmpty()
                       ? new Pair<>(List.of(), new StringFieldValue(value))
                       : new Pair<>(errors, null);
            }

            @Override
            public Pair<List<String>, FieldValue> visit(NumberFieldSchema numberField) {
                val valueOpt = fieldToDouble(fieldData);
                if (valueOpt.isEmpty()) {
                    return new Pair<>(List.of("Field " + fieldName + " is not a number"), null);
                }
                val value = valueOpt.get().doubleValue();
                if (numberField.getMin() != 0 && value < numberField.getMin()) {
                    errors.add("Field value for " + fieldName + " is less than allowed limit " + numberField.getMin());
                }
                if (numberField.getMax() != 0 && value > numberField.getMax()) {
                    errors.add("Field value for " + fieldName + " is grater than allowed limit " + numberField.getMax());
                }
                return errors.isEmpty()
                       ? new Pair<>(List.of(), new NumberFieldValue(value))
                       : new Pair<>(errors, null);
            }

            @Override
            public Pair<List<String>, FieldValue> visit(BooleanFieldSchema booleanField) {
                return fieldToBoolean(fieldData)
                        .<Pair<List<String>, FieldValue>>map(
                                value -> errors.isEmpty() ? new Pair<>(List.of(), new BooleanFieldValue(value))
                                                          : new Pair<>(errors, null))
                        .orElseGet(() -> new Pair<>(List.of("Field " + fieldName + " is not boolean"), null));
            }

            @Override
            public Pair<List<String>, FieldValue> visit(LocationFieldSchema locationField) {
                val latNode = fieldData.get("lat");
                val lonNode = fieldData.get("lon");
                if (latNode.isNull() || !latNode.isDouble()
                        || lonNode.isNull() || !lonNode.isDouble()) {
                    return new Pair<>(List.of("Field " + fieldName + " is not locations with {lat,lon} fields"), null);

                }
                return errors.isEmpty()
                       ? new Pair<>(List.of(), new LocationFieldValue(latNode.asDouble(), lonNode.asDouble()))
                       : new Pair<>(errors, null);
            }

            @Override
            public Pair<List<String>, FieldValue> visit(DateFieldSchema dateField) {
                return fieldToDate(fieldData)
                        .<Pair<List<String>, FieldValue>>map(
                                value -> errors.isEmpty() ? new Pair<>(List.of(), new DateFieldValue(value))
                                                          : new Pair<>(errors, null))
                        .orElseGet(() -> new Pair<>(List.of("Field " + fieldName + " is not a date"), null));
            }

            @Override
            public Pair<List<String>, FieldValue> visit(ChoiceFieldSchema choiceField) {
                val choices = fieldData.isArray()
                              ? StreamSupport.stream(Spliterators.spliteratorUnknownSize(fieldData.elements(),
                                                                                         Spliterator.ORDERED), false)
                                      .filter(JsonNode::isTextual)
                                      .map(JsonNode::asText)
                                      .collect(Collectors.toUnmodifiableSet())
                              : (fieldData.isTextual() ? Set.of(fieldData.asText()) : Set.<String>of());
                if (choices.isEmpty()) {
                    return new Pair<>(List.of("Field " + fieldName + " is not choice or choice list"), null);
                }
                val valid = choiceField.getChoices()
                        .stream()
                        .map(ChoiceFieldSchema.Option::getValue)
                        .collect(Collectors.toUnmodifiableSet());
                if (!valid.containsAll(choices)) {
                    errors.add("Invalid choices " + Sets.difference(choices,
                                                                    valid) + " provided for field " + fieldName);
                }
                return errors.isEmpty()
                       ? new Pair<>(List.of(), new ChoiceFieldValue(List.copyOf(choices)))
                       : new Pair<>(errors, null);
            }
        });
    }

    private static Optional<Double> fieldToDouble(JsonNode fieldData) {

        if (fieldData.isNumber()) {
            return Optional.of(fieldData.asDouble());
        }
        else if (fieldData.isTextual()) {
            try {
                return Optional.of(Double.parseDouble(fieldData.asText()));
            }
            catch (NumberFormatException e) {
                //Don't let this leak
            }
        }
        return Optional.empty();
    }

    private static Optional<Boolean> fieldToBoolean(JsonNode fieldData) {

        if (fieldData.isBoolean()) {
            return Optional.of(fieldData.asBoolean());
        }
        else if (fieldData.isTextual()) {
            return Optional.of(Boolean.parseBoolean(fieldData.asText()));
        }
        return Optional.empty();
    }

    private static Optional<Date> fieldToDate(JsonNode fieldData) {

        if (fieldData.isLong()) {
            return Optional.of(new Date(fieldData.asLong()));
        }
        else if (fieldData.isTextual()) {
            return Optional.of(ConductorServerUtils.htmlDateToDate(fieldData.asText()));
        }
        return Optional.empty();
    }

}
