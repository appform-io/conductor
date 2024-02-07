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

package io.appform.conductor.server.attributes.definition.impl;

import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.attributes.definition.AttributeDefinitionVisitor;
import io.appform.conductor.model.attributes.definition.impl.*;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.server.attributes.definition.AttributeDefinitionStore;
import io.appform.conductor.server.attributes.definition.impl.models.*;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

import static io.appform.conductor.model.error.ConductorErrorCode.*;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBAttributeDefinitionStore implements AttributeDefinitionStore {
    private final RelationalDao<StoredAttributeDefinition> definitionDao;

    @Override
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttributeDefinition.ATTRIBUTE_DEFINITIONS_TABLE))
    public Optional<AttributeDefinition> save(
            @Throws.RuntimeParam("id") AttributeScopeType scopeType,
            @Throws.RuntimeParam("subId") String attributeDefinitionId,
            AttributeDefinition definition) {
        return definitionDao.createOrUpdate(
                        scopeType.name(),
                        criteria(scopeType, attributeDefinitionId),
                        existing -> updateAttributeDef(scopeType, attributeDefinitionId, definition, existing),
                        () -> toStored(definition)
                                .setId(attributeDefinitionId)
                                .setScopeType(scopeType))
                .map(DBAttributeDefinitionStore::toWire);
    }

    @Override
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttributeDefinition.ATTRIBUTE_DEFINITIONS_TABLE))
    public List<AttributeDefinition> readAll(@Throws.RuntimeParam("id") AttributeScopeType scopeType) {
        return definitionDao.select(scopeType.name(),
                                    DetachedCriteria.forClass(StoredAttributeDefinition.class)
                                            .add(Property.forName(StoredAttributeDefinition.Fields.scopeType)
                                                         .eq(scopeType))
                                            .add(Property.forName(StoredAttributeDefinition.Fields.deleted).eq(false))
                                            .addOrder(Order.desc(StoredAttributeDefinition.Fields.name)),
                                    0,
                                    Integer.MAX_VALUE)
                .stream()
                .map(DBAttributeDefinitionStore::toWire)
                .toList();
    }

    @Override
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttributeDefinition.ATTRIBUTE_DEFINITIONS_TABLE))
    public Optional<AttributeDefinition> read(
            @Throws.RuntimeParam("id") AttributeScopeType scopeType,
            @Throws.RuntimeParam("subId") String attributeDefinitionId) {
        return definitionDao.select(scopeType.name(),
                                    criteria(scopeType, attributeDefinitionId),
                                    0,
                                    1)
                .stream()
                .findAny()
                .map(DBAttributeDefinitionStore::toWire);
    }

    @Override
    public boolean delete(
            @Throws.RuntimeParam("id") AttributeScopeType scopeType,
            @Throws.RuntimeParam("subId") String attributeDefinitionId) {
        return definitionDao.update(scopeType.name(),
                                    criteria(scopeType, attributeDefinitionId),
                                    existing -> existing.setDeleted(true));
    }

    private static StoredAttributeDefinition updateAttributeDef(
            AttributeScopeType scopeType,
            String attributeDefinitionId,
            AttributeDefinition definition,
            StoredAttributeDefinition existing) {
        return existing
                .setDisplayName(definition.getDisplayName())
                .setDescription(definition.getDescription())
                .setDeleted(false)
                .accept(new StoredAttributeDefinitionVisitor<StoredAttributeDefinition>() {
                    @Override
                    public StoredAttributeDefinition visit(
                            StoredStringAttributeDefinition stringAttributeDefinition) {
                        if (definition instanceof StringAttributeDefinition strDef) {
                            return stringAttributeDefinition.setPattern(strDef.getPattern())
                                    .setMaxLength(strDef.getMaxLength());
                        }
                        throw new IllegalArgumentException(String.format(
                                "Type mismatch for attribute %s/%s. Expected: %s Received: %s",
                                scopeType,
                                attributeDefinitionId,
                                existing.getType(),
                                definition.getType()));
                    }

                    @Override
                    public StoredAttributeDefinition visit(
                            StoredChoiceAttributeDefinition choiceAttributeDefinition) {
                        if (definition instanceof ChoiceAttributeDefinition choice) {
                            return choiceAttributeDefinition.setOptions(choice.getOptions())
                                    .setAllowMultiple(choice.isAllowMultiple());
                        }
                        throw new IllegalArgumentException(String.format(
                                "Type mismatch for attribute %s/%s. Expected: %s Received: %s",
                                scopeType,
                                attributeDefinitionId,
                                existing.getType(),
                                definition.getType()));
                    }

                    @Override
                    public StoredAttributeDefinition visit(
                            StoredNumberAttributeDefinition numberAttributeDefinition) {
                        if (definition instanceof NumberAttributeDefinition numDef) {
                            return numberAttributeDefinition.setMax(numDef.getMax())
                                    .setMin(numDef.getMin());
                        }
                        throw new IllegalArgumentException(String.format(
                                "Type mismatch for attribute %s/%s. Expected: %s Received: %s",
                                scopeType,
                                attributeDefinitionId,
                                existing.getType(),
                                definition.getType()));
                    }

                    @Override
                    public StoredAttributeDefinition visit(
                            StoredDateAttributeDefinition dateAttributeDefinition) {
                        throw new IllegalArgumentException(String.format(
                                "Type mismatch for attribute %s/%s. Expected: %s Received: %s",
                                scopeType,
                                attributeDefinitionId,
                                existing.getType(),
                                definition.getType()));
                    }

                    @Override
                    public StoredAttributeDefinition visit(
                            StoredLinkAttributeDefinition linkAttributeDefinition) {
                        throw new IllegalArgumentException(String.format(
                                "Type mismatch for attribute %s/%s. Expected: %s Received: %s",
                                scopeType,
                                attributeDefinitionId,
                                existing.getType(),
                                definition.getType()));
                    }
                });
    }

    private static DetachedCriteria criteria(final AttributeScopeType scopeType, final String attributeDefinitionId) {
        return DetachedCriteria.forClass(StoredAttributeDefinition.class)
                .add(Property.forName(StoredAttributeDefinition.Fields.scopeType).eq(scopeType))
                .add(Property.forName(StoredAttributeDefinition.Fields.id).eq(attributeDefinitionId));
    }

    private static StoredAttributeDefinition toStored(final AttributeDefinition definition) {
        return definition.accept(new AttributeDefinitionVisitor<StoredAttributeDefinition>() {
                    @Override
                    public StoredAttributeDefinition visit(StringAttributeDefinition stringAttributeDefinition) {
                        return new StoredStringAttributeDefinition()
                                .setMaxLength(stringAttributeDefinition.getMaxLength())
                                .setPattern(stringAttributeDefinition.getPattern())
                                ;
                    }

                    @Override
                    public StoredAttributeDefinition visit(NumberAttributeDefinition numberAttributeDefinition) {
                        return new StoredNumberAttributeDefinition()
                                .setMax(numberAttributeDefinition.getMax())
                                .setMin(numberAttributeDefinition.getMin());
                    }

                    @Override
                    public StoredAttributeDefinition visit(ChoiceAttributeDefinition choiceAttributeDefinition) {
                        return new StoredChoiceAttributeDefinition()
                                .setOptions(choiceAttributeDefinition.getOptions())
                                .setAllowMultiple(choiceAttributeDefinition.isAllowMultiple());
                    }

                    @Override
                    public StoredAttributeDefinition visit(DateAttributeDefinition dateAttributeDefinition) {
                        return new StoredDateAttributeDefinition();
                    }

                    @Override
                    public StoredAttributeDefinition visit(LinkAttributeDefinition linkAttributeDefinition) {
                        return new StoredLinkAttributeDefinition();
                    }
                })
                .setId(definition.getId())
                .setName(definition.getName())
                .setDisplayName(definition.getDisplayName());
    }

    public static AttributeDefinition toWire(final StoredAttributeDefinition definition) {
        return definition.accept(new StoredAttributeDefinitionVisitor<>() {
            @Override
            public AttributeDefinition visit(StoredStringAttributeDefinition stringAttributeDefinition) {
                return new StringAttributeDefinition(definition.getId(),
                                                     definition.getName(),
                                                     definition.getDisplayName(),
                                                     definition.getDescription(),
                                                     definition.getCreated(),
                                                     definition.getUpdated(),
                                                     stringAttributeDefinition.getMaxLength(),
                                                     stringAttributeDefinition.getPattern());
            }

            @Override
            public AttributeDefinition visit(StoredChoiceAttributeDefinition choiceAttributeDefinition) {
                return new ChoiceAttributeDefinition(definition.getId(),
                                                     definition.getName(),
                                                     definition.getDisplayName(),
                                                     definition.getDescription(),
                                                     definition.getCreated(),
                                                     definition.getUpdated(),
                                                     choiceAttributeDefinition.getOptions(),
                                                     choiceAttributeDefinition.isAllowMultiple());
            }

            @Override
            public AttributeDefinition visit(StoredNumberAttributeDefinition numberAttributeDefinition) {
                return new NumberAttributeDefinition(definition.getId(),
                                                     definition.getName(),
                                                     definition.getDisplayName(),
                                                     definition.getDescription(),
                                                     definition.getCreated(),
                                                     definition.getUpdated(),
                                                     numberAttributeDefinition.getMax(),
                                                     numberAttributeDefinition.getMin());
            }

            @Override
            public AttributeDefinition visit(StoredDateAttributeDefinition dateAttributeDefinition) {
                return new DateAttributeDefinition(definition.getId(),
                                                   definition.getName(),
                                                   definition.getDisplayName(),
                                                   definition.getDescription(),
                                                   definition.getCreated(),
                                                   definition.getUpdated());
            }

            @Override
            public AttributeDefinition visit(StoredLinkAttributeDefinition linkAttributeDefinition) {
                return new LinkAttributeDefinition(definition.getId(),
                                                   definition.getName(),
                                                   definition.getDisplayName(),
                                                   definition.getDescription(),
                                                   definition.getCreated(),
                                                   definition.getUpdated());
            }
        });
    }
}
