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

package io.appform.conductor.user.auth.impl;

import io.appform.conductor.core.auth.RoleStore;
import io.appform.conductor.core.hazelcast.HazelcastClient;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.auth.Role;
import io.appform.conductor.model.error.Throws;
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
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
@Singleton
@Slf4j
public class CachingRoleStore implements RoleStore {
    private final RoleStore root;
    private final Provider<Cache<String, Role>> rolesCacheProvider;

    @Inject
    public CachingRoleStore(
            @Named(Constants.ROOT_IMPLEMENTATION_NAME) final RoleStore root,
            HazelcastClient hazelcastClient) {
        this.root = root;
        this.rolesCacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName(),
                new CacheLoader<String, Role>() {
                    @Override
                    public Role load(String key) throws CacheLoaderException {
                        log.debug("Loading role: {}", key);
                        return root.read(key).orElse(null);
                    }

                    @Override
                    public Map<String, Role> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {

                        val ids = StreamSupport.stream(keys.spliterator(), false)
                                .map(String.class::cast)
                                .collect(Collectors.toUnmodifiableSet());
                        log.debug("Loading roles for {}", ids);
                        return root.list()
                                .stream()
                                .filter(role -> ids.contains(role.getId()))
                                .collect(Collectors.toUnmodifiableMap(Role::getId, Function.identity()));
                    }
                });
    }

    @Override
    @MonitoredFunction
    public Optional<Role> create(
            @Throws.RuntimeParam("id") String roleId,
            String name,
            String description,
            Set<Permission> permissions) {
        return root.create(roleId, name, description, permissions)
                .flatMap(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public Optional<Role> read(String roleId) {
        return Optional.ofNullable(rolesCacheProvider.get().get(roleId));
    }

    @Override
    @MonitoredFunction
    public List<Role> list() {
        return root.list(); //Offload to db, this is not a general use-case
    }

    @Override
    @MonitoredFunction
    public Optional<Role> update(String roleId, UnaryOperator<Role> handler) {
        return root.update(roleId, handler)
                .flatMap(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public boolean delete(String roleId) {
        val status = root.delete(roleId);
        if(status) {
            log.debug("Removing data for deleted role: {}", roleId);
            rolesCacheProvider.get().remove(roleId);
        }
        return status;
    }

    private Optional<Role> refreshCache(final Role role) {
        val cache = rolesCacheProvider.get();
        log.debug("Removing role {} from cache", role.getId());
        cache.remove(role.getId()); //Delete from cache ... let it load organically when read
        return read(role.getId());
    }
}
