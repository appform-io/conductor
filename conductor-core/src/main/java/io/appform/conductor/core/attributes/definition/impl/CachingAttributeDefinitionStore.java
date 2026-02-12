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
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.server.attributes.definition.AttributeDefinitionStore;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.utils.Pair;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
@Singleton
@Slf4j
public class CachingAttributeDefinitionStore implements AttributeDefinitionStore {
    private final AttributeDefinitionStore root;
    private final Provider<Cache<AttributeScopeType, List<AttributeDefinition>>> cacheProvider;

    @Inject
    public CachingAttributeDefinitionStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) AttributeDefinitionStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;
        this.cacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName(),
                new CacheLoader<>() {
                    @Override
                    public List<AttributeDefinition> load(AttributeScopeType key) throws CacheLoaderException {
                        log.info("Loading attribute definitions for {}", key);
                        return root.readAll(key);
                    }

                    @Override
                    public Map<AttributeScopeType, List<AttributeDefinition>> loadAll(
                            Iterable<?
                                    extends AttributeScopeType> keys) throws CacheLoaderException {
                        return StreamSupport.stream(keys.spliterator(), false)
                                .map(key -> new Pair<>(key, root.readAll(key)))
                                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
                    }
                });
    }

    @Override
    public Optional<AttributeDefinition> save(
            AttributeScopeType scopeType,
            String attributeDefinitionId,
            AttributeDefinition definition) {
        val res = root.save(scopeType, attributeDefinitionId, definition);
        res.ifPresent(attrDef -> cacheProvider.get().remove(scopeType));
        return read(scopeType, attributeDefinitionId);
    }

    @Override
    public List<AttributeDefinition> readAll(AttributeScopeType scopeType) {
        return Objects.requireNonNullElse(cacheProvider.get().get(scopeType), List.of());
    }

    @Override
    public Optional<AttributeDefinition> read(AttributeScopeType scopeType, String attributeDefinitionId) {
        return Objects.requireNonNullElse(cacheProvider.get().get(scopeType), List.<AttributeDefinition>of())
                .stream()
                .filter(def -> def.getId().equals(attributeDefinitionId))
                .findAny();
    }

    @Override
    public boolean delete(AttributeScopeType scopeType, String attributeDefinitionId) {
        val status = root.delete(scopeType, attributeDefinitionId);
        return status && cacheProvider.get().remove(scopeType);
    }
}
