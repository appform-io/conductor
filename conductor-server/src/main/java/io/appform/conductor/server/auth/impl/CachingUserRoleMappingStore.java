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

package io.appform.conductor.server.auth.impl;

import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.auth.UserRoleMappingStore;
import io.appform.conductor.server.hazelcast.HazelcastClient;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
@Singleton
@Slf4j
public class CachingUserRoleMappingStore implements UserRoleMappingStore {
    private final UserRoleMappingStore root;
    private final Provider<Cache<String, String>> cacheProvider;

    @Inject
    public CachingUserRoleMappingStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) final UserRoleMappingStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;

        cacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName(),
                new CacheLoader<>() {
                    @Override
                    public String load(String key) throws CacheLoaderException {
                        log.debug("Loading data for user {}", key);
                        return root.roleForUser(key).orElse(null);
                    }

                    @Override
                    public Map<String, String> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                        val ids = StreamSupport.stream(keys.spliterator(), false)
                                .map(String.class::cast)
                                .collect(Collectors.toUnmodifiableSet());
                        log.debug("Loading roles for {}", ids);
                        return ids
                                .stream()
                                .map(uId -> new Pair<>(uId, root.roleForUser(uId).orElse(null)))
                                .filter(pair -> pair.getSecond() != null)
                                .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));
                    }
                });
    }

    @Override
    @MonitoredFunction
    public boolean assignRoleToUser(String userId, String roleId) {
        val status = root.assignRoleToUser(userId, roleId);
        if(status) {
            cacheProvider.get().remove(userId);
            cacheProvider.get().put(userId, roleId);
        }
        return status;
    }

    @Override
    public boolean revokeRoleFromUser(String userId, String roleId) {
        val status = root.revokeRoleFromUser(userId, roleId);
        if(status) {
            cacheProvider.get().remove(userId);
        }
        return status;
    }

    @Override
    public Optional<String> roleForUser(String userId) {
        return Optional.ofNullable(cacheProvider.get().get(userId));
    }
}
