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

package io.appform.conductor.server.skillmanagement.impl;

import io.appform.conductor.model.skills.SkillDefinition;
import io.appform.conductor.model.skills.SkillValue;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.skillmanagement.SkillStore;
import io.appform.conductor.server.utils.Pair;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
@Singleton
@Slf4j
public class CachingSkillStore implements SkillStore {
    private final SkillStore root;
    private final Provider<Cache<String, List<SkillValue>>> cacheProvider;

    @Inject
    public CachingSkillStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) final SkillStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;

        cacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName() + "-user-skills",
                new CacheLoader<>() {
                    @Override
                    public List<SkillValue> load(String key) throws CacheLoaderException {
                        log.debug("Loading skills for user: {}", key);
                        return root.listSkillsForUser(key);
                    }

                    @Override
                    public Map<String, List<SkillValue>> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                        val ids = StreamSupport.stream(keys.spliterator(), false)
                                .map(String.class::cast)
                                .toList();
                        log.debug("Loading skills for users: {}", ids);
                        return ids.stream()
                                .map(id -> new Pair<>(id, root.listSkillsForUser(id)))
                                .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));
                    }
                });
    }

    @Override
    @MonitoredFunction
    public Optional<SkillDefinition> createSkillDefinition(String name) {
        return root.createSkillDefinition(name);
    }

    @Override
    @MonitoredFunction
    public Optional<SkillDefinition> updateSkillDefinition(String skillId, String name) {
        return root.updateSkillDefinition(skillId, name);
    }

    @Override
    @MonitoredFunction
    public Optional<SkillDefinition> addValueToSkillDefinition(String id, String value) {
        return root.addValueToSkillDefinition(id, value);
    }

    @Override
    public Optional<SkillDefinition> removeValueFromSkillDefinition(String id, String valueId) {
        return root.removeValueFromSkillDefinition(id, valueId);
    }

    @Override
    public Optional<SkillDefinition> updateSkillValue(String id, String valueId, String value) {
        return root.updateSkillValue(id, valueId, valueId);
    }

    @Override
    public Optional<SkillDefinition> readSkillDefinition(String id) {
        return root.readSkillDefinition(id);
    }

    @Override
    public Optional<SkillValue> readSkillValue(String id, String valueId) {
        return root.readSkillValue(id, valueId);
    }

    @Override
    public boolean deleteSkillDefinition(String id) {
        return root.deleteSkillDefinition(id);
    }

    @Override
    public List<SkillDefinition> listSkillDefinitions() {
        return root.listSkillDefinitions();
    }

    @Override
    public List<SkillValue> listSkillValues() {
        return root.listSkillValues();
    }

    @Override
    public boolean associateSkillWithUser(String userId, String skillId, String valueId) {
        val status = root.associateSkillWithUser(userId, skillId, valueId);
        if (status) {
            cacheProvider.get().remove(userId);
            if (listSkillsForUser(userId).isEmpty()) {
                //This will load the data
                log.warn("No data found for user {}", userId);
            }
        }
        return status;
    }

    @Override
    public boolean disassociateSkillWithUser(String userId, String skillId, String valueId) {
        val status = root.disassociateSkillWithUser(userId, skillId, valueId);
        if (status) {
            cacheProvider.get().remove(userId);
        }
        return status;
    }

    @Override
    public List<SkillValue> listSkillsForUser(String userId) {
        return Objects.requireNonNullElse(cacheProvider.get().get(userId), List.of());
    }
}
