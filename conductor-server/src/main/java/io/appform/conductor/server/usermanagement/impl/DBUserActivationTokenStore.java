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
import io.appform.conductor.model.usermgmt.UserActivationToken;
import io.appform.conductor.model.usermgmt.UserActivationTokenState;
import io.appform.conductor.server.usermanagement.UserActivationTokenStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserActivationToken;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.val;

import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * An implementation to store {@link UserActivationToken}
 * to RDBMS
 */
public class DBUserActivationTokenStore implements UserActivationTokenStore {

    public static final String ACTIVATION_TOKEN_TABLE_NAME = "user_activation_links";

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
                                     .put("type", ACTIVATION_TOKEN_TABLE_NAME)
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
                                     .put("type", ACTIVATION_TOKEN_TABLE_NAME)
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
                                     .put("type", ACTIVATION_TOKEN_TABLE_NAME)
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
