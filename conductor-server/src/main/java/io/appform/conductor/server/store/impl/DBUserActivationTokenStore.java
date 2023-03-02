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
import io.appform.conductor.model.usermgmt.UserActivationToken;
import io.appform.conductor.model.usermgmt.UserActivationTokenState;
import io.appform.conductor.server.store.UserActivationTokenStore;
import io.appform.conductor.server.utils.DateUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Data;
import lombok.val;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.inject.Inject;
import javax.persistence.*;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * An implementation to store {@link UserActivationToken}
 * to RDBMS
 */
public class DBUserActivationTokenStore implements UserActivationTokenStore {

    private static final String TABLE_NAME = "user_activation_links";

    /**
     * DB model for {@link UserActivationToken}
     */
    @Entity
    @Table(name = TABLE_NAME)
    @Data
    public static class StoredUserActivationToken {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private long id;

        @LookupKey
        @Column(name = "token", unique = true, nullable = false, length = 45)
        private String token;

        @Column(name = "user_id", nullable = false)
        private String userId;

        @Column(name = "email", unique = true, nullable = false)
        private Date validTill;

        @Column(name = "partitionId", nullable = false)
        private int partitionId;

        @Enumerated(EnumType.STRING)
        @Column(name = "state", nullable = false, length = 45)
        private UserActivationTokenState state;

        @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
        @Generated(value = GenerationTime.INSERT)
        private Date created;

        @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
        @Generated(value = GenerationTime.ALWAYS)
        private Date updated;

        public StoredUserActivationToken(
                String token,
                String userId,
                Date validTill,
                UserActivationTokenState state) {
            this.token = token;
            this.userId = userId;
            this.validTill = validTill;
            this.state = state;
            this.partitionId = DateUtils.currentWeek();
        }
    }

    private final LookupDao<StoredUserActivationToken> tokenDao;

    @Inject
    public DBUserActivationTokenStore(LookupDao<StoredUserActivationToken> tokenDao) {
        this.tokenDao = tokenDao;
    }

    @Override
    public Optional<UserActivationToken> generate(String userId, Date validTill) {
        val token = UUID.randomUUID().toString();
        try {
            return tokenDao.save(new StoredUserActivationToken(token,
                                                               userId,
                                                               validTill,
                                                               UserActivationTokenState.UNVALIDATED))
                    .map(DBUserActivationTokenStore::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", TABLE_NAME)
                                     .put("id", token)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Optional<UserActivationToken> getById(String token) {
        try {
            return tokenDao.get(token)
                    .map(DBUserActivationTokenStore::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", TABLE_NAME)
                                     .put("id", token)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public boolean update(String token, Consumer<UserActivationToken> handler) {
        try {
            return tokenDao.update(token, storedToken -> {
                val userToken = storedToken.orElse(null);
                if (userToken != null) {

                    val updatedToken = toWire(userToken);
                    handler.accept(updatedToken);
                    userToken.setState(updatedToken.getState());
                }
                return userToken;
            });
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", TABLE_NAME)
                                     .put("id", token)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    private static UserActivationToken toWire(final StoredUserActivationToken token) {
        return new UserActivationToken(
                token.getToken(),
                token.getUserId(),
                token.getValidTill(),
                token.getState(),
                token.getCreated());
    }

}
