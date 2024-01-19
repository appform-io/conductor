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

import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.usermanagement.UserStore;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
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
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;

/**
 * Caches user data based on user id
 */
@Slf4j
@Singleton
public class CachingUserStore implements UserStore {

    private final UserStore root;
    private final Provider<Cache<String, UserSummary>> cacheProvider;

    @Inject
    public CachingUserStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) final UserStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;
        val cacheName = getClass().getSimpleName();
        cacheProvider = hazelcastClient.getORCreateCache(
                cacheName,
                new MutableConfiguration<String, UserSummary>()
                        .setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> new AccessedExpiryPolicy(Duration.TEN_MINUTES))
                        .setCacheLoaderFactory((Factory<CacheLoader<String, UserSummary>>) () -> new CacheLoader<>() {
                            @Override
                            public UserSummary load(String key) throws CacheLoaderException {
                                log.debug("Loading data for user: {}", key);
                                return root.getById(key).orElse(null);
                            }

                            @Override
                            public Map<String, UserSummary> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                                val userIds = StreamSupport.stream(keys.spliterator(), false)
                                        .map(String.class::cast)
                                        .toList();
                                log.debug("Loading data for users: {}", userIds);
                                return root.getByIds(userIds)
                                        .stream()
                                        .collect(toMap(UserSummary::getId, Function.identity()));
                            }
                        })
                        .setReadThrough(true)
                        .setWriteThrough(false)
                        .setStatisticsEnabled(true),
                cache -> {}); //Can't pre-load all users

    }

    @Override
    public Optional<UserSummary> create(String userId, String name, UserType type, String email) {
        return root.create(userId, name, type, email);
    }

    @Override
    public Optional<UserSummary> getById(String userId) {
        return Optional.ofNullable(cacheProvider.get().get(userId));
    }

    @Override
    public List<UserSummary> getByIds(List<String> userIds) {
        return root.getByIds(userIds); //Bulk get doesn't need to be cached, should not be needed
    }

    @Override
    public Optional<UserSummary> getByEmail(String email) {
        return root.getByEmail(email); //This would be used in auth path, let it go to DB
    }

    @Override
    public Optional<UserSummary> update(String userId, UnaryOperator<UserSummary> handler) {
        val res = root.update(userId, handler);
        if(res.isPresent()) {
            cacheProvider.get().remove(userId); //Remove stale data from cache
        }
        return res;
    }
}
