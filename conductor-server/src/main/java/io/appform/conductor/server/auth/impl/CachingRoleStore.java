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

import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.auth.Role;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.auth.RoleStore;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.cache.Cache;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) final RoleStore root,
            HazelcastClient hazelcastClient) {
        this.root = root;
        this.rolesCacheProvider = hazelcastClient.consistentCache(
                getClass().getSimpleName(),
                cache -> cache.putAll(root.list().stream().collect(Collectors.toMap(Role::getId, Function.identity()))));
    }

    @Override
    @MonitoredFunction
    public Optional<Role> create(
            @Throws.RuntimeParam("id") String roleId,
            String name,
            String description,
            Set<Permission> permissions) {
        return root.create(roleId, name, description, permissions)
                .map(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public Optional<Role> read(String roleId) {
        return Optional.ofNullable(rolesCacheProvider.get().get(roleId));
    }

    @Override
    @MonitoredFunction
    public List<Role> list() {
        return StreamSupport.stream(rolesCacheProvider.get().spliterator(), false)
                .map(Cache.Entry::getValue)
                .sorted(Comparator.comparing(Role::getId))
                .toList();
    }

    @Override
    @MonitoredFunction
    public Optional<Role> update(String roleId, UnaryOperator<Role> handler) {
        return root.update(roleId, handler)
                .map(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public boolean delete(String roleId) {
        val status = root.delete(roleId);
        if(status) {
            rolesCacheProvider.get().remove(roleId);
        }
        return status;
    }

    private Role refreshCache(final Role role) {
        val cache = rolesCacheProvider.get();
        cache.put(role.getId(), role);
        return role;
    }
}
