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
import io.appform.conductor.model.usermgmt.UserActivationToken;
import io.appform.conductor.model.usermgmt.UserActivationTokenState;
import io.appform.conductor.server.usermanagement.UserActivationTokenStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserActivationToken;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * An implementation to store {@link UserActivationToken} to RDBMS
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBUserActivationTokenStore implements UserActivationTokenStore {

    private final LookupDao<StoredUserActivationToken> tokenDao;

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserActivationToken.ACTIVATION_TOKEN_TABLE_NAME))
    public Optional<UserActivationToken> generate(@Throws.RuntimeParam("id") String userId, Date validTill) {
        val token = UUID.randomUUID().toString();
        return tokenDao.save(new StoredUserActivationToken(token,
                                                           userId,
                                                           validTill,
                                                           UserActivationTokenState.UNVALIDATED))
                .map(DBUserActivationTokenStore::toWire);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserActivationToken.ACTIVATION_TOKEN_TABLE_NAME))
    public Optional<UserActivationToken> getById(@Throws.RuntimeParam("id") String token) {
        return tokenDao.get(token)
                .map(DBUserActivationTokenStore::toWire);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserActivationToken.ACTIVATION_TOKEN_TABLE_NAME))
    public boolean update(@Throws.RuntimeParam("id") String token, Consumer<UserActivationToken> handler) {
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

    private static UserActivationToken toWire(final StoredUserActivationToken token) {
        return new UserActivationToken(
                token.getToken(),
                token.getUserId(),
                token.getValidTill(),
                token.getState(),
                token.getCreated());
    }

}
