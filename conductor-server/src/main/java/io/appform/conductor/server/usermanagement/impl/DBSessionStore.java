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

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import io.appform.conductor.server.usermanagement.SessionStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserSessionDetails;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Stores {@link io.appform.conductor.model.usermgmt.UserSessionDetails} in RDBMS
 */
@Slf4j
public class DBSessionStore implements SessionStore {

    private final RelationalDao<StoredUserSessionDetails> sessionDetailsDao;

    @Inject
    public DBSessionStore(RelationalDao<StoredUserSessionDetails> sessionDetailsDao) {
        this.sessionDetailsDao = sessionDetailsDao;
    }

    @Override
    @SneakyThrows
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserSessionDetails.SESSION_TABLE_NAME))
    public Optional<UserSessionDetails> create(@Throws.RuntimeParam("id") String userId, SessionType type, Date expiry) {
        return sessionDetailsDao.save(userId,
                                      new StoredUserSessionDetails(
                                              ConductorServerUtils.generateSessionId(),
                                              userId,
                                              SessionState.ACTIVE,
                                              type,
                                              expiry,
                                              new Date()))
                .map(DBSessionStore::toWire);
    }

    @Override
    @SneakyThrows
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserSessionDetails.SESSION_TABLE_NAME))
    public Optional<UserSessionDetails> getById(String userId, @Throws.RuntimeParam("id") String sessionId) {
        return sessionDetailsDao.select(userId,
                                        sessionCriteria(userId, sessionId),
                                        0,
                                        1)
                .stream()
                .findAny()
                .map(DBSessionStore::toWire);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserSessionDetails.SESSION_TABLE_NAME))
    public Optional<UserSessionDetails> update(
            String userId, @Throws.RuntimeParam("id") String sessionId, Function<UserSessionDetails, UserSessionDetails> handler) {
        val status = sessionDetailsDao.update(userId, sessionCriteria(userId, sessionId), storedSession -> {
            val session = handler.apply(toWire(storedSession));
            storedSession.setState(session.getState());
            storedSession.setLastActive(session.getLastActive());
            return storedSession;
        });
        if(status) {
            log.warn("Session {} for user {} could not be updated", sessionId, userId);
            return getById(userId, sessionId);
        }
        return Optional.empty();
    }

    private static DetachedCriteria sessionCriteria(String userId, String sessionId) {
        return DetachedCriteria.forClass(StoredUserSessionDetails.class)
                .add(Property.forName(StoredUserSessionDetails.Fields.userId).eq(userId))
                .add(Property.forName(StoredUserSessionDetails.Fields.sessionId).eq(sessionId));
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
