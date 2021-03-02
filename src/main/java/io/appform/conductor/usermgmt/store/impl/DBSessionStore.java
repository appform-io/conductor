/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.usermgmt.store.impl;

import com.google.common.collect.ImmutableMap;
import io.appform.conductor.error.ConductorErrorCode;
import io.appform.conductor.error.ConductorException;
import io.appform.conductor.usermgmt.store.SessionStore;
import io.appform.conductor.utils.DateUtils;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.persistence.*;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Stores {@link io.appform.conductor.usermgmt.store.SessionStore.UserSessionDetails} in RDBMS
 */
@Slf4j
public class DBSessionStore implements SessionStore {
    private static final String TABLE_NAME = "user_sessions";

    /**
     * DB model object corresponding to {@link io.appform.conductor.usermgmt.store.SessionStore.UserSessionDetails}
     */
    @Entity
    @Table(name = TABLE_NAME, uniqueConstraints = {
            @UniqueConstraint(name = "uk_sessions", columnNames = {"partition_id", "user_id", "session_id"})
    })
    @Data
    @NoArgsConstructor
    public static class StoredUserSessionDetails {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private long id;

        @Column(name = "session_id", nullable = false, length = 45)
        private String sessionId;

        @Column(name = "user_id", unique = true, nullable = false, length = 45)
        private String userId;

        @Column
        @Enumerated(EnumType.STRING)
        private SessionState state;

        @Column(name = "last_active", nullable = false)
        private Date lastActive;

        @Column(name = "partition_id", nullable = false)
        private int partitionId;

        @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
        @Generated(value = GenerationTime.INSERT)
        private Date created;

        @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
                updatable = false, insertable = false)
        @Generated(value = GenerationTime.ALWAYS)
        private Date updated;

        public StoredUserSessionDetails(
                String sessionId,
                String userId,
                SessionState state, Date lastActive) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.state = state;
            this.lastActive = lastActive;
            this.partitionId = DateUtils.currentWeek();
        }
    }

    private final RelationalDao<StoredUserSessionDetails> sessionDetailsDao;

    @Inject
    public DBSessionStore(RelationalDao<StoredUserSessionDetails> sessionDetailsDao) {
        this.sessionDetailsDao = sessionDetailsDao;
    }

    @Override
    public Optional<UserSessionDetails> create(String userId) {
        try {
            return sessionDetailsDao.save(userId,
                                          new StoredUserSessionDetails(
                                                  UUID.randomUUID().toString(),
                                                  userId,
                                                  SessionState.ACTIVE,
                                                  new Date()))
                    .map(DBSessionStore::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", TABLE_NAME)
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
                                     .put("type", TABLE_NAME)
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
                                     .put("type", TABLE_NAME)
                                     .put("id", String.format("%s-%s", userId, sessionId))
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    private static DetachedCriteria sessionCriteria(String userId, String sessionId) {
        return DetachedCriteria.forClass(StoredUserSessionDetails.class)
                .add(Restrictions.eq("userId", userId))
                .add(Restrictions.eq("sessionId", sessionId));
    }

    private static UserSessionDetails toWire(StoredUserSessionDetails session) {
        return new UserSessionDetails(
                session.getSessionId(),
                session.getUserId(),
                session.getState(),
                session.getLastActive(),
                session.getCreated());
    }
}
