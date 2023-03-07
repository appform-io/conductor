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

package io.appform.conductor.server.store.impl;

import com.google.common.collect.ImmutableMap;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.server.internalmodels.auth.UserPasswordAuthDetails;
import io.appform.conductor.server.store.UserPasswordAuthStore;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.persistence.*;
import java.util.Date;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 *
 */
public class DBUserPasswordAuthStore implements UserPasswordAuthStore {
    private static final String TABLE_NAME = "user_passwords";

    @Inject
    public DBUserPasswordAuthStore(RelationalDao<StoredUserPassword> passwordDao) {
        this.passwordDao = passwordDao;
    }

    @Entity
    @Table(name = TABLE_NAME)
    @Data
    @NoArgsConstructor
    public static class StoredUserPassword {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private long id;

        @Column(name = "user_id", unique = true, nullable = false, length = 45)
        private String userId;

        @Column(name = "password", nullable = false)
        private String password;

        @Column(name = "failed_password_attempt")
        private int failedPasswordAttempts;

        @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
        @Generated(value = GenerationTime.INSERT)
        private Date created;

        @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
                updatable = false, insertable = false)
        @Generated(value = GenerationTime.ALWAYS)
        private Date updated;

        public StoredUserPassword(String userId, String password, int failedPasswordAttempts) {
            this.userId = userId;
            this.password = password;
            this.failedPasswordAttempts = failedPasswordAttempts;
        }
    }

    private final RelationalDao<StoredUserPassword> passwordDao;

    @Override
    public Optional<UserPasswordAuthDetails> set(String userId, String password) {
        try {
            return passwordDao.save(userId, new StoredUserPassword(userId, password, 0))
                    .map(DBUserPasswordAuthStore::toWire);
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
    public Optional<UserPasswordAuthDetails> update(String userId, final UnaryOperator<UserPasswordAuthDetails> updater) {
        try {
            return passwordDao.select(userId, createCriteria(userId), 0, 1)
                    .stream()
                    .findFirst()
                    .map(existing -> {
                        val updated = updater.apply(toWire(existing));
                        return existing.setPassword(updated.getPassword())
                                .setFailedPasswordAttempts(updated.getFailedPasswordAttempts());
                    })
                    .flatMap(updated -> {
                        try {
                            return passwordDao.save(userId, updated);
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
                    })
                    .map(DBUserPasswordAuthStore::toWire);
        }
        catch (Exception e) {
            if (e instanceof ConductorException ce) {
                throw ce;
            }
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
    public Optional<UserPasswordAuthDetails> get(String userId) {
        try {
            return passwordDao.select(userId, createCriteria(userId),
                                      0, 1)
                    .stream()
                    .findFirst()
                    .map(DBUserPasswordAuthStore::toWire);
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

    private static DetachedCriteria createCriteria(String userId) {
        return DetachedCriteria.forClass(StoredUserPassword.class)
                .add(Restrictions.eq("userId", userId));
    }

    private static UserPasswordAuthDetails toWire(final StoredUserPassword password) {
        return new UserPasswordAuthDetails(password.getUserId(),
                                           password.getPassword(),
                                           password.getFailedPasswordAttempts(),
                                           password.getCreated(),
                                           password.getUpdated());
    }
}
