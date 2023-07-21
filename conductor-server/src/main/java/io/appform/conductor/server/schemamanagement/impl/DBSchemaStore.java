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
    public Optional<SchemaSummary> create(@Throws.RuntimeParam("id") String name, String description) {
        val schemaId = lowerSnake(name);
        return schemaDao.save(new StoredSchemaSummary(schemaId,
                                                      name,
                                                      description,
                                                      INACTIVE,
                                                      operatingUserId()))
                .map(DBSchemaStore::toSummary);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<SchemaSummary> getSummary(String schemaId) {
        return schemaDao.get(schemaId).map(DBSchemaStore::toSummary);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
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
    public List<SchemaSummary> list() {
        return schemaDao.scatterGather(DetachedCriteria.forClass(StoredSchemaSummary.class))
                .stream()
                .map(DBSchemaStore::toSummary)
                .toList();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<SchemaSummary> updateDescription(@Throws.RuntimeParam("id") String schemaId, String description) {
        return Optional.ofNullable(schemaDao.lockAndGetExecutor(schemaId)
                                           .mutate(summary -> summary.setDescription(description))
                                           .execute())
                .map(DBSchemaStore::toSummary);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<SchemaSummary> updateState(@Throws.RuntimeParam("id") String schemaId, SchemaState state) {
        return Optional.ofNullable(schemaDao.lockAndGetExecutor(schemaId)
                                           .mutate(summary -> summary.setState(state)
                                                   .setStateChangedBy(operatingUserId()))
                                           .execute())
                .map(DBSchemaStore::toSummary);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.SCHEMA_FIELD_WRITE_FAILED)
    public Optional<FieldSchema> addField(@Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
                                          @Throws.RuntimeParam(StoredFieldSchema.Fields.fieldId) String fieldId,
                                          FieldSchema schema) {
        return fieldDao.save(schemaId, toStoredFieldSchema(schemaId, fieldId, schema))
                .map(this::toFieldSchema);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.SCHEMA_FIELD_READ_FAILED)
    public Optional<FieldSchema> getField(@Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
                                          @Throws.RuntimeParam(StoredFieldSchema.Fields.fieldId) String fieldId) {
        return fieldDao.select(schemaId, DetachedCriteria.forClass(StoredFieldSchema.class)
                        .add(Property.forName(StoredFieldSchema.Fields.fieldId).eq(fieldId)), 0, 1)
                .stream().findFirst().map(this::toFieldSchema);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.SCHEMA_FIELD_WRITE_FAILED)
    public Optional<FieldSchema> updateField(@Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
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
    public boolean deleteField(@Throws.RuntimeParam(StoredFieldSchema.Fields.schemaId) String schemaId,
                               @Throws.RuntimeParam(StoredFieldSchema.Fields.fieldId) String fieldId) {
        return fieldDao.lockAndGetExecutor(schemaId, DetachedCriteria.forClass(StoredFieldSchema.class)
                        .add(Property.forName(StoredFieldSchema.Fields.fieldId).eq(fieldId)))
                .mutate(schema -> schema.setDeleted(true))
                .execute() != null;
    }

    private static SchemaSummary toSummary(final StoredSchemaSummary schemaSummary) {
        return new SchemaSummary(schemaSummary.getSchemaId(),
                                 schemaSummary.getName(),
                                 schemaSummary.getDescription(),
                                 0,
                                 schemaSummary.getState(),
                                 schemaSummary.getStateChangedBy(),
                                 schemaSummary.getCreated(),
                                 schemaSummary.getUpdated());
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
                                             storedField.isRequired(),
                                             storedField.getParent(),
                                             storedField.getVisibilityCondition(),
                                             storedField.getEditableCondition(),
                                             storedField.isAllowMultiple(),
                                             storedField.getCreated(),
                                             storedField.getUpdated(),
                                             stringField.getMaxLength(),
                                             stringField.getMatchPattern(),
                                             stringField.getDefaultValue());
            }

            @Override
            public FieldSchema visit(StoredBooleanFieldSchema booleanField) {
                return new BooleanFieldSchema(storedField.getFieldId(),
                                              storedField.getName(),
                                              storedField.getDisplayName(),
                                              storedField.getDescription(),
                                              storedField.isRequired(),
                                              storedField.getParent(),
                                              storedField.getVisibilityCondition(),
                                              storedField.getEditableCondition(),
                                              storedField.isAllowMultiple(),
                                              storedField.getCreated(),
                                              storedField.getUpdated(),
                                              booleanField.isDefaultValue());
            }

            @Override
            public FieldSchema visit(StoredLocationFieldSchema locationField) {
                return new LocationFieldSchema(storedField.getFieldId(),
                                               storedField.getName(),
                                               storedField.getDisplayName(),
                                               storedField.getDescription(),
                                               storedField.isRequired(),
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
                                           storedField.isRequired(),
                                           storedField.getParent(),
                                           storedField.getVisibilityCondition(),
                                           storedField.getEditableCondition(),
                                           storedField.isAllowMultiple(),
                                           storedField.getCreated(),
                                           storedField.getUpdated(),
                                           dateField.getDefaultValue());
            }

            @Override
            public FieldSchema visit(StoredNumberFieldSchema numberField) {
                return new NumberFieldSchema(storedField.getFieldId(),
                                             storedField.getName(),
                                             storedField.getDisplayName(),
                                             storedField.getDescription(),
                                             storedField.isRequired(),
                                             storedField.getParent(),
                                             storedField.getVisibilityCondition(),
                                             storedField.getEditableCondition(),
                                             storedField.isAllowMultiple(),
                                             storedField.getCreated(),
                                             storedField.getUpdated(),
                                             numberField.getMax(),
                                             numberField.getMin(),
                                             numberField.getDefaultValue());
            }

            @SneakyThrows
            @Override
            public FieldSchema visit(StoredChoiceFieldSchema choiceField) {
                return new ChoiceFieldSchema(storedField.getFieldId(),
                                             storedField.getName(),
                                             storedField.getDisplayName(),
                                             storedField.getDescription(),
                                             storedField.isRequired(),
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
                                                   fieldSchema.isRequired(),
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
                                                   fieldSchema.isRequired(),
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
                                                    fieldSchema.isRequired(),
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
                                                     fieldSchema.isRequired(),
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
                                                 fieldSchema.isRequired(),
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
                                                   fieldSchema.isRequired(),
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
                .setDisplayName(schema.getDisplayName())
                .setDescription(updated.getDescription())
                .setRequired(updated.isRequired())
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
                                .setDefaultValue(updatedStringField.getDefaultValue());
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
                                .setDefaultValue(updatedNumberField.getDefaultValue());
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
                        booleanField.setDefaultValue(booleanField.isDefaultValue());
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
                                .setDefaultValue(updateDateField.getDefaultValue());
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
