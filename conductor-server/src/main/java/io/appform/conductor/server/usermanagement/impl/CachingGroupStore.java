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

package io.appform.conductor.server.usermanagement.impl;

import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
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
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) GroupStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;
        this.groupCacheProvider = hazelcastClient.getORCreateCache(
                getClass().getSimpleName() + "-groups",
                cache -> cache.putAll(root.list()
                                              .stream()
                                              .collect(Collectors.toUnmodifiableMap(Group::getId,
                                                                                    Function.identity()))));
        userGroupsCacheProvider = hazelcastClient.getORCreateCache(
                getClass().getSimpleName() + "-user-groups",
                new MutableConfiguration<String, List<Group>>()
                        .setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> new TouchedExpiryPolicy(Duration.TEN_MINUTES))
                        .setCacheLoaderFactory((Factory<CacheLoader<String, List<Group>>>) () -> new CacheLoader<>() {
                            @Override
                            public List<Group> load(String key) throws CacheLoaderException {
                                log.debug("Loading data for user: {}", key);
                                return root.findGroupsForUser(key);
                            }

                            @Override
                            public Map<String, List<Group>> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                                throw new UnsupportedOperationException("Can't send load multiple data"); //TODO
                            }
                        })
                        .setReadThrough(true)
                        .setWriteThrough(false)
                        .setStatisticsEnabled(true),
                cache -> {}); //Can't pre-load all users

    }

    @Override
    @MonitoredFunction
    public Optional<Group> create(String name, String description, GroupType type, Set<String> requiredSkills) {
        return root.create(name, description, type, requiredSkills)
                .map(this::replaceGroupInCache);
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
            groupCacheProvider.get().remove(groupId);
        }
        return status;
    }

    @Override
    @MonitoredFunction
    public Optional<Group> update(String groupId, UnaryOperator<Group> handler) {
        return root.update(groupId, handler)
                .map(this::replaceGroupInCache);
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
        return StreamSupport.stream(groupCacheProvider.get().spliterator(), false)
                .map(Cache.Entry::getValue)
                .sorted(Comparator.comparing(Group::getId))
                .toList();
    }

    private Group replaceGroupInCache(Group group) {
        Objects.requireNonNull(group.getId());
        val cache = groupCacheProvider.get();
        cache.put(group.getId(), group);
        return group;
    }
}
