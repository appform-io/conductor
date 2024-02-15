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

package io.appform.conductor.server.schemamanagement.impl;

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchemaSummary;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.cache.Cache;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
@Slf4j
@Singleton
public class CachingSchemaStore implements SchemaStore {
    private final SchemaStore root;
    private final Provider<Cache<String, Schema>> cacheProvider;

    @Inject
    public CachingSchemaStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) SchemaStore root,
            HazelcastClient hazelcastClient) {
        this.root = root;
        val cacheName = getClass().getSimpleName();
        this.cacheProvider = hazelcastClient.loadingCache(
                cacheName,
                new CacheLoader<>() {
                    @Override
                    public Schema load(String key) throws CacheLoaderException {
                        log.debug("Reading schema for {}", key);
                        return root.read(key).orElse(null);
                    }

                    @Override
                    public Map<String, Schema> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                        val ids = StreamSupport.stream(keys.spliterator(), false)
                                .map(String.class::cast)
                                .collect(Collectors.toUnmodifiableSet());
                        log.debug("Loading schema for {}", ids);
                        return root.list()
                                .stream()
                                .filter(schema -> ids.contains(schema.getId()))
                                .collect(Collectors.toUnmodifiableMap(Schema::getId, Function.identity()));
                    }
                });

    }

    @Override
    @MonitoredFunction
    public Optional<Schema> create(String name, String description) {
        return root.create(name, description)
                .flatMap(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<Schema> read(@Throws.RuntimeParam("id") String schemaId) {
        return Optional.ofNullable(cacheProvider.get().get(schemaId));
    }

    @Override
    @MonitoredFunction
    public List<Schema> list() {
        return root.list(); //Offload to DB
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> updateDescription(String schemaId, String description) {
        return root.updateDescription(schemaId, description)
                .flatMap(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> updateState(String schemaId, SchemaState state) {
        return root.updateState(schemaId, state)
                .flatMap(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public Optional<FieldSchema> addField(String schemaId, String fieldId, FieldSchema schema) {
        return root.addField(schemaId, fieldId, schema)
                .flatMap(fieldSchema -> updatedFieldSchema(schemaId, fieldId, fieldSchema));
    }

    @Override
    @MonitoredFunction
    public Optional<FieldSchema> getField(String schemaId, String fieldId) {
        return read(schemaId)
                .flatMap(s -> fieldFromSchema(fieldId, s));
    }

    @Override
    @MonitoredFunction
    public Optional<FieldSchema> updateField(String schemaId, String fieldId, FieldSchema updated) {
        return root.updateField(schemaId, fieldId, updated)
                .flatMap(fieldSchema -> updatedFieldSchema(schemaId, fieldId, fieldSchema));
    }

    @Override
    @MonitoredFunction
    public boolean deleteField(String schemaId, String fieldId) {
        val status = root.deleteField(schemaId, fieldId);
        if (status) {
            cacheProvider.get().remove(schemaId);
        }
        return status;
    }

    private Optional<FieldSchema> updatedFieldSchema(String schemaId, String fieldId, FieldSchema fieldSchema) {
        if (fieldSchema != null) {
            log.debug("Removing data for schema due to field update {}", schemaId);
            cacheProvider.get().remove(schemaId);
            return getField(schemaId, fieldId);
        }
        return Optional.empty();
    }

    private Optional<Schema> refreshCache(Schema schema) {
        if (null != schema) {
            val cache = this.cacheProvider.get();
            log.debug("Removing data for schema {}", schema.getId());
            cache.remove(schema.getId()); //Let it load organically
            return read(schema.getId());
        }
        return Optional.empty();
    }

    private static Optional<FieldSchema> fieldFromSchema(String fieldId, Schema s) {
        return s.getFields()
                .stream()
                .filter(fld -> fld.getId().equals(fieldId))
                .findAny();
    }
}
