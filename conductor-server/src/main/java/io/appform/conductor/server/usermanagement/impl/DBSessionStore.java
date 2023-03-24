/*
 * Copyright (c) 2023 Santanu Sinha
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

import com.google.common.collect.ImmutableMap;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import io.appform.conductor.server.usermanagement.SessionStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserSessionDetails;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Stores {@link io.appform.conductor.model.usermgmt.UserSessionDetails} in RDBMS
 */
@Slf4j
public class DBSessionStore implements SessionStore {
    public static final String SESSION_TABLE_NAME = "user_sessions";

    private final RelationalDao<StoredUserSessionDetails> sessionDetailsDao;

    @Inject
    public DBSessionStore(RelationalDao<StoredUserSessionDetails> sessionDetailsDao) {
        this.sessionDetailsDao = sessionDetailsDao;
    }

    @Override
    public Optional<UserSessionDetails> create(String userId, SessionType type, Date expiry) {
        try {
            return sessionDetailsDao.save(userId,
                                          new StoredUserSessionDetails(
                                                  UUID.randomUUID().toString(),
                                                  userId,
                                                  SessionState.ACTIVE,
                                                  type,
                                                  expiry,
                                                  new Date()))
                    .map(DBSessionStore::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SESSION_TABLE_NAME)
                                     .put("id", userId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Optional<UserSessionDetails> getById(String userId, String sessionId) {
        try {
            return sessionDetailsDao.select(userId,
                                            sessionCriteria(userId, sessionId),
                                            0,
                                            1)
                    .stream()
                    .findAny()
                    .map(DBSessionStore::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SESSION_TABLE_NAME)
                                     .put("id", String.format("%s-%s", userId, sessionId))
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Optional<UserSessionDetails> update(
            String userId, String sessionId, Consumer<UserSessionDetails> handler) {
        try {
            val status = sessionDetailsDao.update(userId, sessionCriteria(userId, sessionId), storedSession -> {
                val session = toWire(storedSession);
                handler.accept(session);
                storedSession.setState(session.getState());
                storedSession.setLastActive(session.getLastActive());
                return storedSession;
            });
            if(status) {
                log.warn("Session {} for user {} could not be updated", sessionId, userId);
            }
            return Optional.empty();
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", SESSION_TABLE_NAME)
                                     .put("id", String.format("%s-%s", userId, sessionId))
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    private static DetachedCriteria sessionCriteria(String userId, String sessionId) {
        return DetachedCriteria.forClass(StoredUserSessionDetails.class)
                .add(Property.forName("userId").eq(userId))
                .add(Property.forName("sessionId").eq(sessionId));
    }

    private static UserSessionDetails toWire(StoredUserSessionDetails session) {
        return new UserSessionDetails(
                session.getSessionId(),
                session.getUserId(),
                session.getState(),
                session.getType(),
                session.getExpiry(),
                session.getLastActive(),
                session.getCreated());
    }
}
