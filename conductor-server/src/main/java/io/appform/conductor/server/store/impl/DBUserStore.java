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

package io.appform.conductor.server.store.impl;

import com.google.common.collect.ImmutableMap;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.server.store.UserStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.sharding.LookupKey;
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
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * RDBMS backend for {@link UserStore}
 */
@Slf4j
public class DBUserStore implements UserStore {
    private static final String TABLE_NAME = "users";

    /**
     * DB model object corresponding to {@link UserSummary}
     */
    @Entity
    @Table(name = TABLE_NAME)
    @Data
    @NoArgsConstructor
    public static class StoredUser {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private long id;

        @LookupKey
        @Column(name = "user_id", unique = true, nullable = false, length = 45)
        private String userId;

        @Column(name = "user_type", unique = false, nullable = false, length = 45)
        private UserType userType;

        @Column(name = "name", nullable = false)
        private String name;

        @Column(name = "email", unique = true, nullable = false)
        private String email;

        @Column
        @Enumerated(EnumType.STRING)
        private UserState state;

        @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
        @Generated(value = GenerationTime.INSERT)
        private Date created;

        @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
                updatable = false, insertable = false)
        @Generated(value = GenerationTime.ALWAYS)
        private Date updated;

        public StoredUser(
                String userId,
                UserType userType,
                String name,
                String email,
                UserState state) {
            this.userId = userId;
            this.userType = userType;
            this.name = name;
            this.email = email;
            this.state = state;
        }
    }
    
    private final LookupDao<StoredUser> userDao;

    @Inject
    public DBUserStore(LookupDao<StoredUser> userDao) {
        this.userDao = userDao;
    }

    @Override
    public Optional<UserSummary> create(String name, UserType userType, String email) {
        final String userId = ConductorServerUtils.normalize(name);
        try {
            return userDao.save(
                    new StoredUser(
                            userId,
                            userType,
                            name,
                            email,
                            UserState.CREATED))
                    .map(DBUserStore::toWire);
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
    public Optional<UserSummary> getById(String userId) {
        try {
            return userDao.get(userId)
                    .map(DBUserStore::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", TABLE_NAME)
                                     .put("id", userId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public List<UserSummary> getByIds(List<String> userIds) {
        try {
            return userDao.get(userIds)
                    .stream()
                    .map(DBUserStore::toWire)
                    .toList();
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_LIST_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", TABLE_NAME)
                                     .build())
                    .cause(e)
                    .build();
        }    }

    @Override
    public Optional<UserSummary> getByEmail(String email) {
        try {
            return userDao.scatterGather(
                    DetachedCriteria.forClass(StoredUser.class)
                            .add(Restrictions.eq("email", email)))
                    .stream()
                    .findAny()
                    .map(DBUserStore::toWire);
        }
        catch(Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                            .put("type", TABLE_NAME)
                            .build())
                    .cause(e)
                    .build();
        }

    }

    @Override
    public Optional<UserSummary> update(
            String userId, UnaryOperator<UserSummary> handler) {
        try {
            boolean updatedResult = userDao.update(userId, userOptional -> {
                val user = userOptional.orElse(null);
                if (user == null) {
                    return null;
                }
                val updatedUser = handler.apply(toWire(user));
                user.setEmail(updatedUser.getEmail())
                        .setName(updatedUser.getName())
                        .setState(updatedUser.getState());
                return user;
            });
            log.info("Update result for user: {} is: {}", userId, updatedResult);
            return getById(userId);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_UPDATE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                            .put("type", TABLE_NAME)
                            .put("id", userId)
                            .build())
                    .cause(e)
                    .build();
        }
    }

    private static UserSummary toWire(StoredUser user) {
        return new UserSummary(
                user.getUserId(),
                user.getUserType(),
                user.getName(),
                user.getEmail(),
                user.getState(),
                user.getCreated(),
                user.getUpdated());
    }

}
