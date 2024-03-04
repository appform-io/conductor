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

package io.appform.conductor.server.attributes.values.impl;

import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.value.AttributeValue;
import io.appform.conductor.model.attributes.value.AttributeValueVisitor;
import io.appform.conductor.model.attributes.value.impl.*;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.attributes.values.AttributeValueStore;
import io.appform.conductor.server.attributes.values.impl.models.*;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_RELATED_ENTITY_READ_ERROR;
import static io.appform.conductor.model.error.ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR;

/**
 *
 */
@Singleton
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBAttributeValueStore implements AttributeValueStore {

    private final RelationalDao<StoredAttributeValue> valueDao;

    @Override
    @MonitoredFunction
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttributeValue.ATTRIBUTE_VALUES_TABLE))
    public List<AttributeValue> save(
            @Throws.RuntimeParam("id") AttributeScopeType scopeType,
            @Throws.RuntimeParam("subId") String objectRefId,
            List<AttributeValue> attributes) {
        val stored = toStored(scopeType,
                              objectRefId,
                              attributes
                             );
        val status = valueDao.saveAll(shardingKey(scopeType, objectRefId), stored);
        if(!status) {
            log.error("Could not save attributes for {}/{}", scopeType, objectRefId);
        }
        return read(scopeType, objectRefId);
    }

    @Override
    @SneakyThrows
    @MonitoredFunction
    @Throws(value = STORE_RELATED_ENTITY_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttributeValue.ATTRIBUTE_VALUES_TABLE))
    public List<AttributeValue> read(
            @Throws.RuntimeParam("id") AttributeScopeType scopeType,
            @Throws.RuntimeParam("subId") String objectRefId) {
        return valueDao.select(shardingKey(scopeType, objectRefId),
                               DetachedCriteria.forClass(StoredAttributeValue.class)
                                       .add(Property.forName(StoredAttributeValue.Fields.scopeType).eq(scopeType))
                                       .add(Property.forName(StoredAttributeValue.Fields.objectRefId).eq(objectRefId)),
                               0,
                               Integer.MAX_VALUE)
                .stream()
                .map(this::toSingleAttributeValue)
                .toList();
    }

    private String shardingKey(AttributeScopeType scopeType, String objId) {
        return scopeType.name() + "-" + objId;
    }

    public List<StoredAttributeValue> toStored(
            final AttributeScopeType scopeType,
            final String objectRefId,
            final List<AttributeValue> values) {
        return values.stream()
                .map(attributeValue -> toSingleStoredAttribute(scopeType, objectRefId, attributeValue)
                    )
                .toList();
    }

    private static StoredAttributeValue toSingleStoredAttribute(
            AttributeScopeType scopeType,
            String objectRefId,
            AttributeValue attributeValue) {
        return attributeValue.accept(new AttributeValueVisitor<StoredAttributeValue>() {
                    @Override
                    public StoredAttributeValue visit(StringAttributeValue stringAttributeValue) {
                        return new StoredStringAttributeValue()
                                .setTextValue(stringAttributeValue.getValue());
                    }

                    @Override
                    public StoredAttributeValue visit(NumberAttributeValue numberAttributeValue) {
                        return new StoredNumberAttributeValue()
                                .setNumberValue(numberAttributeValue.getValue());
                    }

                    @Override
                    public StoredAttributeValue visit(ChoiceAttributeValue choiceAttributeValue) {
                        return new StoredChoiceAttributeValue()
                                .setChoiceValue(choiceAttributeValue.getValue());
                    }

                    @Override
                    public StoredAttributeValue visit(DateAttributeValue dateAttributeValue) {
                        return new StoredDateAttributeValue()
                                .setDateValue(dateAttributeValue.getValue());
                    }

                    @Override
                    public StoredAttributeValue visit(LinkAttributeValue linkAttributeValue) {
                        return new StoredLinkAttributeValue()
                                .setLinkText(linkAttributeValue.getText())
                                .setUrl(linkAttributeValue.getValue());
                    }
                })
                .setId(ConductorServerUtils.readableId(scopeType.name(),
                                                       objectRefId,
                                                       attributeValue.getSchemaId()))
                .setScopeType(scopeType)
                .setObjectRefId(objectRefId)
                .setAttrDefId(attributeValue.getSchemaId());
    }

    private AttributeValue toSingleAttributeValue(final StoredAttributeValue attributeValue) {
        return attributeValue.accept(new StoredAttributeValueVisitor<AttributeValue>() {
            @Override
            public AttributeValue visit(StoredStringAttributeValue stringAttributeValue) {
                return new StringAttributeValue(
                        stringAttributeValue.getAttrDefId(),
                        stringAttributeValue.getCreated(),
                        stringAttributeValue.getUpdated(),
                        stringAttributeValue.getTextValue()
                );
            }

            @Override
            public AttributeValue visit(StoredNumberAttributeValue numberAttributeValue) {
                return new NumberAttributeValue(
                        numberAttributeValue.getAttrDefId(),
                        numberAttributeValue.getCreated(),
                        numberAttributeValue.getUpdated(),
                        numberAttributeValue.getNumberValue()
                );
            }

            @Override
            public AttributeValue visit(StoredChoiceAttributeValue choiceAttributeValue) {
                return new ChoiceAttributeValue(
                        choiceAttributeValue.getAttrDefId(),
                        choiceAttributeValue.getCreated(),
                        choiceAttributeValue.getUpdated(),
                        choiceAttributeValue.getChoiceValue()
                );
            }

            @Override
            public AttributeValue visit(StoredDateAttributeValue dateAttributeValue) {
                return new DateAttributeValue(
                        dateAttributeValue.getAttrDefId(),
                        dateAttributeValue.getCreated(),
                        dateAttributeValue.getUpdated(),
                        dateAttributeValue.getDateValue()
                );
            }

            @Override
            public AttributeValue visit(StoredLinkAttributeValue linkAttributeValue) {
                return new LinkAttributeValue(
                        linkAttributeValue.getAttrDefId(),
                        linkAttributeValue.getCreated(),
                        linkAttributeValue.getUpdated(),
                        linkAttributeValue.getLinkText(),
                        linkAttributeValue.getUrl()
                );
            }
        });
    }
}
