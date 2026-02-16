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

import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import static io.appform.conductor.core.utils.Constants.ROOT_IMPLEMENTATION_NAME;
import io.appform.conductor.core.hazelcast.HazelcastClient;
import io.appform.conductor.user.usermanagement.SessionStore;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.Value;
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
 * Distributed loading cache for sessions
 */
@Singleton
@Slf4j
public class CachingSessionStore implements SessionStore {

    @Value
    private static class SessionKey {
        String userId;
        String sessionId;
    }

    private final SessionStore root;
    private final Provider<Cache<SessionKey, UserSessionDetails>> sessionCacheProvider;

    @Inject
    public CachingSessionStore(
            @Named(Constants.ROOT_IMPLEMENTATION_NAME) final SessionStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;

        sessionCacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName(),
                new CacheLoader<>() {
                    @Override
                    public UserSessionDetails load(SessionKey key) throws CacheLoaderException {
                        log.debug("Loading data for session {}", key);
                        return root.getById(key.getUserId(),
                                            key.getSessionId())
                                .orElse(null);
                    }

                    @Override
                    public Map<SessionKey, UserSessionDetails> loadAll(
                            Iterable<? extends SessionKey> keys) throws CacheLoaderException {
                        val ids = StreamSupport.stream(keys.spliterator(),
                                                       false)
                                .toList();
                        log.debug("Loading data for sessions {}", ids);
                        return ids.stream()
                                .map(sessionKey -> root.getById(sessionKey.getUserId(),
                                                                sessionKey.getSessionId())
                                        .orElse(null))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toUnmodifiableMap(
                                        CachingSessionStore::keyFromSession,
                                        Function.identity()));
                    }
                });
    }

    @Override
    @MonitoredFunction
    public Optional<UserSessionDetails> create(String userId, SessionType type, Date expiry) {
        return root.create(userId, type, expiry)
                .flatMap(this::refreshCache);
    }

    @Override
    @MonitoredFunction
    public Optional<UserSessionDetails> getById(String userId, String sessionId) {
        return Optional.ofNullable(sessionCacheProvider.get().get(new SessionKey(userId, sessionId)));
    }

    @Override
    @MonitoredFunction
    public List<UserSessionDetails> list(String userId, Set<SessionState> requiredStates) {
        return root.list(userId, requiredStates);
    }

    @Override
    @MonitoredFunction
    public Optional<UserSessionDetails> update(
            String userId,
            String sessionId,
            UnaryOperator<UserSessionDetails> handler) {
        return root.update(userId, sessionId, handler)
                .flatMap(this::refreshCache);
    }

    private Optional<UserSessionDetails> refreshCache(final UserSessionDetails sessionDetails) {
        sessionCacheProvider.get().remove(keyFromSession(sessionDetails));
        return getById(sessionDetails.getUserId(), sessionDetails.getId());
    }

    private static SessionKey keyFromSession(UserSessionDetails sessionDetails) {
        return new SessionKey(sessionDetails.getUserId(), sessionDetails.getId());
    }
}
