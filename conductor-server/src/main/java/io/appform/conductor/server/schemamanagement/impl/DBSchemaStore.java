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
import com.google.common.collect.ImmutableMap;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchema;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static io.appform.conductor.model.schema.SchemaState.ACTIVE;
import static io.appform.conductor.model.schema.SchemaState.INACTIVE;
import static io.appform.conductor.server.schemamanagement.impl.models.StoredSchema.DefinitionType.RAW;
import static io.appform.conductor.server.utils.ConductorServerUtils.normalize;
import static io.appform.conductor.server.utils.ConductorServerUtils.operatingUserId;

/**
 * Stored schema in mysql
 */
public class DBSchemaStore implements SchemaStore {
    public static final String SCHEMA_TABLE_NAME = "schemas";

    private static final String SCHEMA_ID_FIELD = "schemaId";
    private static final String VERSION_FIELD = "version";
    private static final String STATE_FIELD = "state";

    private final RelationalDao<StoredSchema> schemaDao;
    private final ObjectMapper mapper;

    @Inject
    public DBSchemaStore(RelationalDao<StoredSchema> schemaDao, ObjectMapper mapper) {
        this.schemaDao = schemaDao;
        this.mapper = mapper;
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> create(String name, String description, List<FieldSchema> fields) {
        val schemaId = normalize(name);
        try {
            val version = versionNumber(schemaId);
            val storedSchema = new StoredSchema()
                    .setSchemaId(schemaId)
                    .setName(name)
                    .setDescription(description)
                    .setVersion(version)
                    .setDefinitionType(RAW)
                    .setFields(mapper.writeValueAsBytes(fields))
                    .setState(INACTIVE)
                    .setCreatedBy(operatingUserId());
            return schemaDao.save(schemaId, storedSchema).map(this::toWire);
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
    @MonitoredFunction
    public Optional<Schema> get(String schemaId) {
        try {
            return schemaDao.select(schemaId,
                                    DetachedCriteria.forClass(StoredSchema.class)
                                            .add(Property.forName(SCHEMA_ID_FIELD).eq(schemaId))
                                            .add(Property.forName(VERSION_FIELD)
                                                         .eq(DetachedCriteria.forClass(StoredSchema.class)
                                                                     .add(Property.forName(SCHEMA_ID_FIELD).eq(schemaId))
                                                                     .add(Property.forName(STATE_FIELD).eq(ACTIVE))
                                                                     .setProjection(Projections.max(VERSION_FIELD))))
                                            ,
                                    0, 1)
                    .stream()
                    .findFirst()
                    .map(this::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId + "/latest")
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> getVersion(String schemaId, long version) {
        try {
            return schemaDao.select(schemaId,
                                    criteriaForVersion(schemaId, version), 0, 1)
                    .stream()
                    .findFirst()
                    .map(this::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId + "/" + version)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> updateDescription(String schemaId, long version, String description) {
        try {
            val status = schemaDao.update(schemaId,
                                          criteriaForVersion(schemaId, version),
                                          existing -> existing.setDescription(description)); //No version change
            if (status) {
                return getVersion(schemaId, version);
            }
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId + "/" + version)
                                     .build())
                    .cause(e)
                    .build();
        }
        throw ConductorException.builder()
                .errorCode(ConductorErrorCode.SCHEMA_UPDATE_FAILED)
                .context(ImmutableMap.<String, Object>builder()
                                 .put(SCHEMA_ID_FIELD, schemaId)
                                 .put(VERSION_FIELD, version)
                                 .put("operation", "Description update to: " + description)
                                 .build())
                .build();
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> updateState(String schemaId, long version, SchemaState required, SchemaState newState) {
        try {
            val status = schemaDao.update(schemaId,
                                          criteriaForVersion(schemaId, version)
                                                  .add(Property.forName("state").eq(required)),
                                          existing -> existing.setState(newState)
                                                  .setStateChangedBy(operatingUserId())); //Do not update version here
            if (status) {
                return getVersion(schemaId, version);
            }
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId + "/" + version)
                                     .build())
                    .cause(e)
                    .build();
        }
        throw ConductorException.builder()
                .errorCode(ConductorErrorCode.SCHEMA_UPDATE_FAILED)
                .context(ImmutableMap.<String, Object>builder()
                                 .put(SCHEMA_ID_FIELD, schemaId)
                                 .put(VERSION_FIELD, version)
                                 .put("operation", "State update from " + required + " to " + newState)
                                 .build())
                .build();
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> updateFields(String schemaId, long version, List<FieldSchema> fields) {
        try {
            val existing = schemaDao.select(schemaId, criteriaForVersion(schemaId, version), 0, 1)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (null != existing) {
                val versionNumber = versionNumber(schemaId);
                val storedSchema = new StoredSchema()
                        .setSchemaId(schemaId)
                        .setName(existing.getName())
                        .setDescription(existing.getDescription())
                        .setVersion(versionNumber)
                        .setDefinitionType(RAW)
                        .setFields(mapper.writeValueAsBytes(fields))
                        .setState(INACTIVE)
                        .setCreatedBy(operatingUserId());
                return schemaDao.save(schemaId, storedSchema).map(this::toWire); //Do not update version here
            }
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SCHEMA_TABLE_NAME)
                                     .put("id", schemaId + "/" + version)
                                     .build())
                    .cause(e)
                    .build();
        }
        throw ConductorException.builder()
                .errorCode(ConductorErrorCode.SCHEMA_FIELD_WRITE_FAILED)
                .context(ImmutableMap.<String, Object>builder()
                                 .put(SCHEMA_ID_FIELD, schemaId)
                                 .put(VERSION_FIELD, version)
                                 .build())
                .build();
    }

    @SneakyThrows
    private Schema toWire(final StoredSchema schema) {
        return new Schema(schema.getSchemaId(),
                          schema.getName(),
                          schema.getDescription(),
                          schema.getVersion(),
                          schema.getState(),
                          schema.getCreatedBy(),
                          schema.getStateChangedBy(),
                          mapper.readValue(schema.getFields(), new TypeReference<>() {
                          }),
                          schema.getCreated(),
                          schema.getUpdated());
    }

    private long versionNumber(String schemaId) throws Exception {
        return schemaDao.select(schemaId,
                                criteriaForMaxVersion(schemaId),
                                0, 1)
                .stream()
                .findFirst()
                .map(schema -> schema.getVersion() + 1)
                .orElse(1L);
    }

    private static DetachedCriteria criteriaForVersion(String schemaId, long version) {
        return DetachedCriteria.forClass(StoredSchema.class)
                .add(Property.forName(SCHEMA_ID_FIELD).eq(schemaId))
                .add(Property.forName(VERSION_FIELD).eq(version));
    }

    private static DetachedCriteria criteriaForMaxVersion(String schemaId) {
        return DetachedCriteria.forClass(StoredSchema.class)
                .add(Property.forName(SCHEMA_ID_FIELD).eq(schemaId))
                .add(Property.forName(VERSION_FIELD)
                             .eq(DetachedCriteria.forClass(StoredSchema.class)
                                         .add(Property.forName(SCHEMA_ID_FIELD).eq(schemaId))
                                         .setProjection(Projections.max(VERSION_FIELD))));
    }

}
