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

import com.google.common.collect.ImmutableMap;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.*;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.server.schemamanagement.impl.models.*;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

import static io.appform.conductor.model.schema.SchemaState.INACTIVE;
import static io.appform.conductor.server.utils.ConductorServerUtils.normalize;
import static io.appform.conductor.server.utils.ConductorServerUtils.operatingUserId;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBSchemaStore2 implements SchemaStore2 {
    private static final String SCHEMA_TABLE_NAME = "schema_summaries";

    private final LookupDao<StoredSchemaSummary> schemaDao;
    private final RelationalDao<StoredFieldSchema> fieldDao;

    @Override
    public Optional<SchemaSummary> create(String name, String description) {
        val schemaId = normalize(name);
        try {
            return schemaDao.save(new StoredSchemaSummary(schemaId,
                                                          name,
                                                          description,
                                                          INACTIVE,
                                                          operatingUserId()))
                    .map(DBSchemaStore2::toSummary);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Optional<SchemaSummary> getSummary(String schemaId) {
        try {
            return schemaDao.get(schemaId).map(DBSchemaStore2::toSummary);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Optional<Schema> get(String schemaId) {
        try {
            return schemaDao.readOnlyExecutor(schemaId)
                    .readAugmentParent(fieldDao,
                                       DetachedCriteria.forClass(StoredFieldSchema.class)
                                               .add(Property.forName("deleted").eq(false))
                                               .add(Property.forName("schemaId").eq(schemaId)),
                                       0, Integer.MAX_VALUE,
                                       StoredSchemaSummary::setFields)
                    .execute()
                    .map(DBSchemaStore2::toSchema);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId)
                                     .build())
                    .cause(e)
                    .build();
        }

    }

    @Override
    public Optional<SchemaSummary> updateDescription(String schemaId, String description) {
        return Optional.empty();
    }

    @Override
    public Optional<SchemaSummary> updateState(String schemaId, SchemaState state) {
        return Optional.empty();
    }

    @Override
    public Optional<FieldSchema> addField(String schemaId, FieldSchema schema) {
        val fieldId = schemaId + "-" + normalize(schema.getName());
        try {
            return fieldDao.save(schemaId, toStoredFieldSchema(schemaId, fieldId, schema))
                    .map(DBSchemaStore2::toFieldSchema);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.SCHEMA_FIELD_WRITE_FAILED)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("schemaId", schemaId)
                                     .put("fieldId", schemaId)
                                     .put("id", schemaId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public boolean delete(String schemaId, String fieldId) {
        return false;
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

    private static Schema toSchema(final StoredSchemaSummary schema) {
        val fields = schema.getFields() == null
                     ? List.<FieldSchema>of()
                     : schema.getFields()
                             .stream()
                             .map(DBSchemaStore2::toFieldSchema)
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

    private static FieldSchema toFieldSchema(StoredFieldSchema storedField) {
        if(null == storedField) {
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
        });
    }

    private static StoredFieldSchema toStoredFieldSchema(final String schemaId,
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
            public StoredFieldSchema visit(ChoiceFieldSchema choiceField) {
                return null;
            }
        });
    }
}
