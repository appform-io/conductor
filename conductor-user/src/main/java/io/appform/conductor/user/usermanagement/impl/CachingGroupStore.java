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

package io.appform.conductor.user.usermanagement.impl;

import io.appform.conductor.core.hazelcast.HazelcastClient;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.core.interfaces.GroupStore;
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
public class CachingGroupStore implements GroupStore {

    private final GroupStore root;
    private final Provider<Cache<String, Group>> groupCacheProvider;
    private final Provider<Cache<String, List<Group>>> userGroupsCacheProvider;

    @Inject
    public CachingGroupStore(
            @Named(Constants.ROOT_IMPLEMENTATION_NAME) final GroupStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;
        this.groupCacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName() + "-groups",
                new CacheLoader<>() {
                    @Override
                    public Group load(String key) throws CacheLoaderException {
                        log.debug("Loading data for group {}", key);
                        return root.read(key).orElse(null);
                    }

                    @Override
                    public Map<String, Group> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                        val gIds = StreamSupport.stream(keys.spliterator(), false)
                                .map(String.class::cast)
                                .toList();
                        log.debug("Loading data for groups: {}", gIds);
                        return root.read(gIds)
                                .stream()
                                .collect(Collectors.toUnmodifiableMap(Group::getId, Function.identity()));
                    }
                });

        userGroupsCacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName() + "-user-groups",
                new CacheLoader<>() {
                    @Override
                    public List<Group> load(String key) throws CacheLoaderException {
                        log.debug("Loading data for user: {}", key);
                        return root.findGroupsForUser(key);
                    }

                    @Override
                    public Map<String, List<Group>> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                        throw new UnsupportedOperationException("Can't send load multiple data"); //TODO
                    }
                });

    }

    @Override
    @MonitoredFunction
    public Optional<Group> create(String name, String description, GroupType type, Set<String> requiredSkills) {
        return root.create(name, description, type, requiredSkills)
                .flatMap(this::refreshData);
    }


    @Override
    @MonitoredFunction
    public Optional<Group> read(String groupId) {
        return Optional.ofNullable(groupCacheProvider.get().get(groupId));
    }

    @Override
    @MonitoredFunction
    public List<Group> read(List<String> groupIds) {
        return List.copyOf(groupCacheProvider.get().getAll(Set.copyOf(groupIds)).values());
    }

    @Override
    @MonitoredFunction
    public boolean delete(String groupId) {
        val status = root.delete(groupId);
        if (status) {
            log.debug("Removing data for deleted group: {}", groupId);
            groupCacheProvider.get().remove(groupId);
        }
        return status;
    }

    @Override
    @MonitoredFunction
    public Optional<Group> update(String groupId, UnaryOperator<Group> handler) {
        return root.update(groupId, handler)
                .flatMap(this::refreshData);
    }

    @Override
    @MonitoredFunction
    public boolean addUserToGroup(String groupId, String userId) {
        val status = root.addUserToGroup(groupId, userId);
        if (status) {
            userGroupsCacheProvider.get().remove(userId); //Will load when needed
        }
        return status;
    }

    @Override
    @MonitoredFunction
    public boolean removeUserFromGroup(String groupId, String userId) {
        val status = root.removeUserFromGroup(groupId, userId);
        if (status) {
            userGroupsCacheProvider.get().remove(userId); //Will load when needed
        }
        return status;
    }

    @Override
    @MonitoredFunction
    public List<String> findUsersForGroup(String groupId, int start, int limit) {
        return root.findUsersForGroup(groupId, start, limit); //Let it go to db not worth caching
    }

    @Override
    @MonitoredFunction
    public List<Group> findGroupsForUser(String userId) {
        return userGroupsCacheProvider.get().get(userId);
    }

    @Override
    @MonitoredFunction
    public List<Group> list() {
        return root.list(); //Let this go to DB. This is not called many times
    }

    private Optional<Group> refreshData(Group group) {
        Objects.requireNonNull(group.getId());
        val cache = groupCacheProvider.get();
        log.debug("Removing data for group: {}", group.getId());
        cache.remove(group.getId()); //Let it load organically
        return read(group.getId());
    }
}
