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
import io.appform.conductor.usermgmt.model.UserState;
import io.appform.conductor.usermgmt.store.UserStore;
import io.appform.conductor.utils.StringUtils;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * RDBMS backend for {@link UserStore}
 */
@Slf4j
public class DBUserStore implements UserStore {
    private static final String TABLE_NAME = "users";

    /**
     * DB model object corresponding to {@link UserStore.UserDetails}
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

        @Column(name = "name", nullable = false)
        private String name;

        @Column(name = "email", unique = true, nullable = false)
        private String email;

        @Column(name = "password", nullable = false)
        private String password;

        @Column
        @Enumerated(EnumType.STRING)
        private UserState state;

        @Column(name = "failed_password_attempt")
        private int failedPasswordAttempts;

        @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
        @Generated(value = GenerationTime.INSERT)
        private Date created;

        @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
                updatable = false, insertable = false)
        @Generated(value = GenerationTime.ALWAYS)
        private Date updated;

        public StoredUser(
                String userId,
                String name,
                String email,
                String password,
                UserState state,
                int failedPasswordAttempts) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.password = password;
            this.state = state;
            this.failedPasswordAttempts = failedPasswordAttempts;
        }
    }

    private final LookupDao<StoredUser> userDao;

    @Inject
    public DBUserStore(LookupDao<StoredUser> userDao) {
        this.userDao = userDao;
    }

    @Override
    public Optional<UserDetails> create(String name, String email, String password) {
        final String userId = StringUtils.normalize(name);
        try {
            return userDao.save(
                    new StoredUser(
                            userId,
                            name,
                            email,
                            password,
                            UserState.CREATED,
                            0))
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
    public Optional<UserDetails> getById(String userId) {
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
    public List<UserDetails> getByIds(List<String> userIds) {
        try {
            return userDao.get(userIds)
                    .stream()
                    .map(DBUserStore::toWire)
                    .collect(Collectors.toList());
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
    public Optional<UserDetails> getByEmail(String email) {
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
    public Optional<UserDetails> update(
            String userId, Consumer<UserDetails> handler) {
        boolean updatedResult = userDao.update(userId, userOptional -> {
            val user = userOptional.orElse(null);
            if (user == null) {
                return null;
            }
            val updatedUser = toWire(user);
            handler.accept(updatedUser);
            user.setName(updatedUser.getName());
            user.setPassword(updatedUser.getPassword());
            user.setState(updatedUser.getState());
            user.setFailedPasswordAttempts(updatedUser.getFailedPasswordAttempts());
            return user;
        });
        log.info("Update result for user: {} is: {}", userId, updatedResult);
        return getById(userId);
    }

    private static UserDetails toWire(StoredUser user) {
        return new UserDetails(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getState(),
                user.getFailedPasswordAttempts(),
                user.getCreated(),
                user.getUpdated());
    }

}
