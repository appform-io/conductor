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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
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
        this.cacheProvider = hazelcastClient.consistentCache(
                cacheName,
                cache -> root.list()
                        .forEach(wf -> cache.put(wf.getId(), wf)));

    }

    @Override
    @MonitoredFunction
    public Optional<Schema> create(String name, String description) {
        return root.create(name, description).map(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public Optional<Schema> get(@Throws.RuntimeParam("id") String schemaId) {
        return Optional.ofNullable(cacheProvider.get().get(schemaId));
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = "cached-" + StoredSchemaSummary.SCHEMA_TABLE_NAME))
    public List<Schema> list() {
        return StreamSupport.stream(cacheProvider.get().spliterator(), false)
                .map(Cache.Entry::getValue)
                .toList();
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> updateDescription(String schemaId, String description) {
        return root.updateDescription(schemaId, description)
                .map(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public Optional<Schema> updateState(String schemaId, SchemaState state) {
        return root.updateState(schemaId, state)
                .map(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public Optional<FieldSchema> addField(String schemaId, String fieldId, FieldSchema schema) {
        return root.addField(schemaId, fieldId, schema)
                .map(f -> refreshCache(root.get(schemaId).orElse(null)))
                .flatMap(s -> fieldFromSchema(fieldId, s));
    }

    @Override
    @MonitoredFunction
    public Optional<FieldSchema> getField(String schemaId, String fieldId) {
        return get(schemaId)
                .flatMap(s -> fieldFromSchema(fieldId, s));
    }

    @Override
    @MonitoredFunction
    public Optional<FieldSchema> updateField(String schemaId, String fieldId, FieldSchema updated) {
        return root.updateField(schemaId, fieldId, updated)
                .map(f -> refreshCache(root.get(schemaId).orElse(null)))
                .flatMap(s -> fieldFromSchema(fieldId, s));
    }

    @Override
    @MonitoredFunction
    public boolean deleteField(String schemaId, String fieldId) {
        val status = root.deleteField(schemaId, fieldId);
        if (status) {
            refreshCache(root.get(schemaId).orElse(null));
        }
        return status;
    }

    private Schema refreshCache(Schema schema) {
        if (null != schema) {
            val cache = this.cacheProvider.get();
            cache.put(schema.getId(), schema);
            return cache.get(schema.getId());
        }
        return null;
    }

    private static Optional<FieldSchema> fieldFromSchema(String fieldId, Schema s) {
        return s.getFields()
                .stream()
                .filter(fld -> fld.getId().equals(fieldId))
                .findAny();
    }
}
