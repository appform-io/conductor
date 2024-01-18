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

package io.appform.conductor.server.schemamanagement.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.*;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.server.schemamanagement.impl.models.*;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.appform.conductor.model.schema.SchemaState.INACTIVE;
import static io.appform.conductor.server.utils.ConductorServerUtils.lowerSnake;
import static io.appform.conductor.server.utils.ConductorServerUtils.operatingUserId;

/**
 * DB based implementation for {@link SchemaStore}
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBSchemaStore implements SchemaStore {

    private final LookupDao<StoredSchemaSummary> schemaDao;
    private final RelationalDao<StoredFieldSchema> fieldDao;
    private final ObjectMapper mapper;

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<Schema> create(@Throws.RuntimeParam("id") String name, String description) {
        val schemaId = lowerSnake(name);
        return schemaDao.save(new StoredSchemaSummary(schemaId,
                                                      name,
                                                      description,
                                                      INACTIVE,
                                                      operatingUserId()))
                .map(this::toSchema);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = "cached-" + StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<Schema> get(@Throws.RuntimeParam("id") String schemaId) {
        return schemaDao.readOnlyExecutor(schemaId)
                .readAugmentParent(fieldDao,
                                   DetachedCriteria.forClass(StoredFieldSchema.class)
                                           .add(Property.forName(StoredFieldSchema.Fields.deleted).eq(false))
                                           .add(Property.forName(StoredFieldSchema.Fields.schemaId).eq(schemaId)),
                                   0, Integer.MAX_VALUE,
                                   StoredSchemaSummary::setFields)
                .execute()
                .map(this::toSchema);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = "cached-" + StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public List<Schema> list() {
        return schemaDao.scatterGather(DetachedCriteria.forClass(StoredSchemaSummary.class))
                .stream()
                .map(this::toSchema)
                .toList();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<Schema> updateDescription(@Throws.RuntimeParam("id") String schemaId, String description) {
        return Optional.ofNullable(schemaDao.lockAndGetExecutor(schemaId)
                                           .mutate(summary -> summary.setDescription(description))
                                           .execute())
                .map(this::toSchema);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<Schema> updateState(@Throws.RuntimeParam("id") String schemaId, SchemaState state) {
        return Optional.ofNullable(schemaDao.lockAndGetExecutor(schemaId)
                                           .mutate(summary -> summary.setState(state)
                                                   .setStateChangedBy(operatingUserId()))
                                           .execute())
                .map(this::toSchema);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.SCHEMA_FIELD_WRITE_FAILED)
    public Optional<FieldSchema> addField(
            @Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
            @Throws.RuntimeParam(StoredFieldSchema.Fields.fieldId) String fieldId,
            FieldSchema schema) {
        val storedSchema = toStoredFieldSchema(schemaId, fieldId, schema);
        return fieldDao.createOrUpdate(
                        schemaId,
                        DetachedCriteria.forClass(StoredFieldSchema.class)
                                .add(Property.forName(StoredFieldSchema.Fields.schemaId).eq(schemaId))
                                .add(Property.forName(StoredFieldSchema.Fields.fieldId).eq(fieldId)),
                        existing -> {
                            ConductorServerUtils.ensure(storedSchema.getType().equals(existing.getType()),
                                                        ConductorErrorCode.SCHEMA_FIELD_UPDATE_TYPE_MISMATCH,
                                                        Map.of(
                                                                "schemaId", schemaId,
                                                                "fieldId", fieldId,
                                                                "oldType", existing.getType(),
                                                                "newType", storedSchema.getType()
                                                              ));
                            return existing
                                    .setDisplayName(storedSchema.getDisplayName())
                                    .setDescription(storedSchema.getDescription())
                                    .setParent(storedSchema.getParent())
                                    .setAllowMultiple(storedSchema.isAllowMultiple())
                                    .setEditableCondition(storedSchema.getEditableCondition())
                                    .setVisibilityCondition(storedSchema.getVisibilityCondition())
                                    .setDeleted(false)
                                    .accept(new StoredFieldSchemaVisitor<>() {
                                        @Override
                                        public StoredFieldSchema visit(StoredStringFieldSchema stringField) {
                                            val newSchema = (StoredStringFieldSchema) storedSchema;
                                            return stringField.setDefaultString(newSchema.getDefaultString())
                                                    .setMaxLength(newSchema.getMaxLength())
                                                    .setMatchPattern(newSchema.getMatchPattern());
                                        }

                                        @Override
                                        public StoredFieldSchema visit(StoredBooleanFieldSchema booleanField) {
                                            val newSchema = (StoredBooleanFieldSchema) storedSchema;
                                            return booleanField.setDefaultBoolean(newSchema.isDefaultBoolean());
                                        }

                                        @Override
                                        public StoredFieldSchema visit(StoredLocationFieldSchema locationField) {
                                            val newSchema = (StoredLocationFieldSchema) storedSchema;
                                            return locationField
                                                    .setDefaultLat(newSchema.getDefaultLat())
                                                    .setDefaultLon(newSchema.getDefaultLon());
                                        }

                                        @Override
                                        public StoredFieldSchema visit(StoredDateFieldSchema dateField) {
                                            val newSchema = (StoredDateFieldSchema) storedSchema;
                                            return dateField.setDefaultDate(newSchema.getDefaultDate());
                                        }

                                        @Override
                                        public StoredFieldSchema visit(StoredNumberFieldSchema numberField) {
                                            val newSchema = (StoredNumberFieldSchema) storedSchema;
                                            return numberField
                                                    .setMin(newSchema.getMin())
                                                    .setMax(newSchema.getMax())
                                                    .setDefaultNumber(newSchema.getDefaultNumber());
                                        }

                                        @Override
                                        public StoredFieldSchema visit(StoredChoiceFieldSchema choiceField) {
                                            val newSchema = (StoredChoiceFieldSchema) storedSchema;
                                            return choiceField.setDefaultSelection(newSchema.getDefaultSelection())
                                                    .setOptionsData(newSchema.getOptionsData());
                                        }
                                    });
                        },
                        () -> storedSchema)
                .map(this::toFieldSchema);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.SCHEMA_FIELD_READ_FAILED)
    public Optional<FieldSchema> getField(
            @Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
            @Throws.RuntimeParam(StoredFieldSchema.Fields.fieldId) String fieldId) {
        return fieldDao.select(schemaId, DetachedCriteria.forClass(StoredFieldSchema.class)
                        .add(Property.forName(StoredFieldSchema.Fields.fieldId).eq(fieldId)), 0, 1)
                .stream().findFirst().map(this::toFieldSchema);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.SCHEMA_FIELD_WRITE_FAILED)
    public Optional<FieldSchema> updateField(
            @Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
            @Throws.RuntimeParam(StoredFieldSchema.Fields.fieldId) String fieldId,
            FieldSchema updated) {
        return Optional.ofNullable(fieldDao.lockAndGetExecutor(schemaId,
                                                               DetachedCriteria.forClass(StoredFieldSchema.class)
                                                                       .add(Property.forName(StoredFieldSchema.Fields.fieldId)
                                                                                    .eq(fieldId)))
                                           .mutate(schema -> updateSchema(updated, schema))
                                           .execute())
                .map(this::toFieldSchema);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.SCHEMA_FIELD_WRITE_FAILED)
    public boolean deleteField(
            @Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
            @Throws.RuntimeParam(StoredFieldSchema.Fields.fieldId) String fieldId) {
        return fieldDao.lockAndGetExecutor(schemaId, DetachedCriteria.forClass(StoredFieldSchema.class)
                        .add(Property.forName(StoredFieldSchema.Fields.fieldId).eq(fieldId)))
                .mutate(schema -> schema.setDeleted(true))
                .execute() != null;
    }

    private Schema toSchema(final StoredSchemaSummary schema) {
        val fields = schema.getFields() == null
                     ? List.<FieldSchema>of()
                     : schema.getFields()
                             .stream()
                             .map(this::toFieldSchema)
                             .toList();
        return new Schema(schema.getSchemaId(),
                          schema.getName(),
                          schema.getDescription(),
                          0,
                          schema.getState(),
                          schema.getCreatedBy(),
                          schema.getStateChangedBy(),
                          fields,
                          schema.getCreated(),
                          schema.getUpdated());
    }

    private FieldSchema toFieldSchema(StoredFieldSchema storedField) {
        if (null == storedField) {
            return null;
        }
        return storedField.accept(new StoredFieldSchemaVisitor<>() {
            @Override
            public FieldSchema visit(StoredStringFieldSchema stringField) {
                return new StringFieldSchema(storedField.getFieldId(),
                                             storedField.getName(),
                                             storedField.getDisplayName(),
                                             storedField.getDescription(),
                                             storedField.getParent(),
                                             storedField.getVisibilityCondition(),
                                             storedField.getEditableCondition(),
                                             storedField.isAllowMultiple(),
                                             storedField.getCreated(),
                                             storedField.getUpdated(),
                                             stringField.getMaxLength(),
                                             stringField.getMatchPattern(),
                                             stringField.getDefaultString());
            }

            @Override
            public FieldSchema visit(StoredBooleanFieldSchema booleanField) {
                return new BooleanFieldSchema(storedField.getFieldId(),
                                              storedField.getName(),
                                              storedField.getDisplayName(),
                                              storedField.getDescription(),
                                              storedField.getParent(),
                                              storedField.getVisibilityCondition(),
                                              storedField.getEditableCondition(),
                                              storedField.isAllowMultiple(),
                                              storedField.getCreated(),
                                              storedField.getUpdated(),
                                              booleanField.isDefaultBoolean());
            }

            @Override
            public FieldSchema visit(StoredLocationFieldSchema locationField) {
                return new LocationFieldSchema(storedField.getFieldId(),
                                               storedField.getName(),
                                               storedField.getDisplayName(),
                                               storedField.getDescription(),
                                               storedField.getParent(),
                                               storedField.getVisibilityCondition(),
                                               storedField.getEditableCondition(),
                                               storedField.isAllowMultiple(),
                                               storedField.getCreated(),
                                               storedField.getUpdated(),
                                               locationField.getDefaultLat(),
                                               locationField.getDefaultLon());
            }

            @Override
            public FieldSchema visit(StoredDateFieldSchema dateField) {
                return new DateFieldSchema(storedField.getFieldId(),
                                           storedField.getName(),
                                           storedField.getDisplayName(),
                                           storedField.getDescription(),
                                           storedField.getParent(),
                                           storedField.getVisibilityCondition(),
                                           storedField.getEditableCondition(),
                                           storedField.isAllowMultiple(),
                                           storedField.getCreated(),
                                           storedField.getUpdated(),
                                           dateField.getDefaultDate());
            }

            @Override
            public FieldSchema visit(StoredNumberFieldSchema numberField) {
                return new NumberFieldSchema(storedField.getFieldId(),
                                             storedField.getName(),
                                             storedField.getDisplayName(),
                                             storedField.getDescription(),
                                             storedField.getParent(),
                                             storedField.getVisibilityCondition(),
                                             storedField.getEditableCondition(),
                                             storedField.isAllowMultiple(),
                                             storedField.getCreated(),
                                             storedField.getUpdated(),
                                             numberField.getMax(),
                                             numberField.getMin(),
                                             numberField.getDefaultNumber());
            }

            @SneakyThrows
            @Override
            public FieldSchema visit(StoredChoiceFieldSchema choiceField) {
                return new ChoiceFieldSchema(storedField.getFieldId(),
                                             storedField.getName(),
                                             storedField.getDisplayName(),
                                             storedField.getDescription(),
                                             storedField.getParent(),
                                             storedField.getVisibilityCondition(),
                                             storedField.getEditableCondition(),
                                             storedField.isAllowMultiple(),
                                             storedField.getCreated(),
                                             storedField.getUpdated(),
                                             mapper.readValue(choiceField.getOptionsData(),
                                                              new TypeReference<>() {
                                                              }),
                                             choiceField.getDefaultSelection());
            }
        });
    }

    private StoredFieldSchema toStoredFieldSchema(
            final String schemaId,
            String fieldId,
            final FieldSchema fieldSchema) {
        return fieldSchema.accept(new FieldSchemaVisitor<>() {
            @Override
            public StoredFieldSchema visit(StringFieldSchema stringField) {
                return new StoredStringFieldSchema(schemaId,
                                                   fieldId,
                                                   fieldSchema.getName(),
                                                   fieldSchema.getDisplayName(),
                                                   fieldSchema.getDescription(),
                                                   fieldSchema.getParent(),
                                                   fieldSchema.getVisibilityCondition(),
                                                   fieldSchema.getEditableCondition(),
                                                   fieldSchema.isAllowMultiple(),
                                                   stringField.getMaxLength(),
                                                   stringField.getMatchPattern(),
                                                   stringField.getDefaultValue());
            }

            @Override
            public StoredFieldSchema visit(NumberFieldSchema numberField) {
                return new StoredNumberFieldSchema(schemaId,
                                                   fieldId,
                                                   fieldSchema.getName(),
                                                   fieldSchema.getDisplayName(),
                                                   fieldSchema.getDescription(),
                                                   fieldSchema.getParent(),
                                                   fieldSchema.getVisibilityCondition(),
                                                   fieldSchema.getEditableCondition(),
                                                   fieldSchema.isAllowMultiple(),
                                                   numberField.getMax(),
                                                   numberField.getMin(),
                                                   numberField.getDefaultValue());
            }

            @Override
            public StoredFieldSchema visit(BooleanFieldSchema booleanField) {
                return new StoredBooleanFieldSchema(schemaId,
                                                    fieldId,
                                                    fieldSchema.getName(),
                                                    fieldSchema.getDisplayName(),
                                                    fieldSchema.getDescription(),
                                                    fieldSchema.getParent(),
                                                    fieldSchema.getVisibilityCondition(),
                                                    fieldSchema.getEditableCondition(),
                                                    fieldSchema.isAllowMultiple(),
                                                    booleanField.isDefaultValue());
            }

            @Override
            public StoredFieldSchema visit(LocationFieldSchema locationField) {
                return new StoredLocationFieldSchema(schemaId,
                                                     fieldId,
                                                     fieldSchema.getName(),
                                                     fieldSchema.getDisplayName(),
                                                     fieldSchema.getDescription(),
                                                     fieldSchema.getParent(),
                                                     fieldSchema.getVisibilityCondition(),
                                                     fieldSchema.getEditableCondition(),
                                                     fieldSchema.isAllowMultiple(),
                                                     locationField.getDefaultLat(),
                                                     locationField.getDefaultLon());
            }

            @Override
            public StoredFieldSchema visit(DateFieldSchema dateField) {
                return new StoredDateFieldSchema(schemaId,
                                                 fieldId,
                                                 fieldSchema.getName(),
                                                 fieldSchema.getDisplayName(),
                                                 fieldSchema.getDescription(),
                                                 fieldSchema.getParent(),
                                                 fieldSchema.getVisibilityCondition(),
                                                 fieldSchema.getEditableCondition(),
                                                 fieldSchema.isAllowMultiple(),
                                                 dateField.getDefaultValue());
            }

            @Override
            @SneakyThrows
            public StoredFieldSchema visit(ChoiceFieldSchema choiceField) {
                return new StoredChoiceFieldSchema(schemaId,
                                                   fieldId,
                                                   fieldSchema.getName(),
                                                   fieldSchema.getDisplayName(),
                                                   fieldSchema.getDescription(),
                                                   fieldSchema.getParent(),
                                                   fieldSchema.getVisibilityCondition(),
                                                   fieldSchema.getEditableCondition(),
                                                   fieldSchema.isAllowMultiple(),
                                                   mapper.writeValueAsBytes(choiceField.getChoices()),
                                                   choiceField.getDefaultSelection());
            }
        });
    }


    private void updateSchema(FieldSchema updated, StoredFieldSchema schema) {
        schema
                .setDisplayName(updated.getDisplayName())
                .setDescription(updated.getDescription())
                .setParent(updated.getParent())
                .setVisibilityCondition(updated.getVisibilityCondition())
                .setEditableCondition(updated.getEditableCondition())
                .setAllowMultiple(updated.isAllowMultiple());
        updated.accept(new FieldSchemaVisitor<Void>() {
            @Override
            public Void visit(StringFieldSchema updatedStringField) {
                schema.accept(new StoredFieldSchemaVisitorAdapter<Void>() {

                    @Override
                    public Void visit(StoredStringFieldSchema stringField) {
                        stringField
                                .setMaxLength(updatedStringField.getMaxLength())
                                .setMatchPattern(updatedStringField.getMatchPattern())
                                .setDefaultString(updatedStringField.getDefaultValue());
                        return super.visit(stringField);
                    }
                });
                return null;
            }

            @Override
            public Void visit(NumberFieldSchema updatedNumberField) {
                schema.accept(new StoredFieldSchemaVisitorAdapter<Void>() {
                    @Override
                    public Void visit(StoredNumberFieldSchema numberField) {
                        numberField
                                .setMax(updatedNumberField.getMax())
                                .setMin(updatedNumberField.getMin())
                                .setDefaultNumber(updatedNumberField.getDefaultValue());
                        return super.visit(numberField);
                    }
                });
                return null;
            }

            @Override
            public Void visit(BooleanFieldSchema updatedBooleanField) {
                schema.accept(new StoredFieldSchemaVisitorAdapter<Void>() {
                    @Override
                    public Void visit(StoredBooleanFieldSchema booleanField) {
                        booleanField.setDefaultBoolean(booleanField.isDefaultBoolean());
                        return super.visit(booleanField);
                    }
                });
                return null;
            }

            @Override
            public Void visit(LocationFieldSchema updatedLocationField) {
                schema.accept(new StoredFieldSchemaVisitorAdapter<Void>() {
                    @Override
                    public Void visit(StoredLocationFieldSchema locationField) {
                        locationField
                                .setDefaultLat(updatedLocationField.getDefaultLat())
                                .setDefaultLon(updatedLocationField.getDefaultLon());
                        return super.visit(locationField);
                    }
                });
                return null;
            }

            @Override
            public Void visit(DateFieldSchema updateDateField) {
                schema.accept(new StoredFieldSchemaVisitorAdapter<Void>() {
                    @Override
                    public Void visit(StoredDateFieldSchema dateField) {
                        dateField
                                .setDefaultDate(updateDateField.getDefaultValue());
                        return super.visit(dateField);
                    }
                });
                return null;
            }

            @Override
            public Void visit(ChoiceFieldSchema updatedChoiceField) {
                schema.accept(new StoredFieldSchemaVisitorAdapter<Void>() {
                    @Override
                    @SneakyThrows
                    public Void visit(StoredChoiceFieldSchema choiceField) {
                        choiceField
                                .setOptionsData(mapper.writeValueAsBytes(updatedChoiceField.getChoices()))
                                .setDefaultSelection(updatedChoiceField.getDefaultSelection());
                        return super.visit(choiceField);
                    }
                });
                return null;
            }
        });

    }
}
