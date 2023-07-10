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
import io.appform.conductor.server.internalmodels.auth.UserPasswordAuthDetails;
import io.appform.conductor.server.usermanagement.UserPasswordAuthStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserPassword;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 *
 */
public class DBUserPasswordAuthStore implements UserPasswordAuthStore {

    @Inject
    public DBUserPasswordAuthStore(RelationalDao<StoredUserPassword> passwordDao) {
        this.passwordDao = passwordDao;
    }

    private final RelationalDao<StoredUserPassword> passwordDao;

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserPassword.USER_PASSWORD_TABLE_NAME))
    public Optional<UserPasswordAuthDetails> set(@Throws.RuntimeParam("id") String userId, String password) {
        return passwordDao.save(userId, new StoredUserPassword(userId, password, 0))
                .map(DBUserPasswordAuthStore::toWire);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserPassword.USER_PASSWORD_TABLE_NAME))
    public Optional<UserPasswordAuthDetails> update(@Throws.RuntimeParam("id") String userId, final UnaryOperator<UserPasswordAuthDetails> updater) {
        return passwordDao.select(userId, createCriteria(userId), 0, 1)
                .stream()
                .findFirst()
                .map(existing -> {
                    val updated = updater.apply(toWire(existing));
                    return existing.setPassword(updated.getPassword())
                            .setFailedPasswordAttempts(updated.getFailedPasswordAttempts());
                })
                .flatMap(updated -> saveInternal(userId, updated))
                .map(DBUserPasswordAuthStore::toWire);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserPassword.USER_PASSWORD_TABLE_NAME))
    public Optional<UserPasswordAuthDetails> get(@Throws.RuntimeParam("id") String userId) {
        return passwordDao.select(userId, createCriteria(userId),
                                  0, 1)
                .stream()
                .findFirst()
                .map(DBUserPasswordAuthStore::toWire);
    }

    private static DetachedCriteria createCriteria(String userId) {
        return DetachedCriteria.forClass(StoredUserPassword.class)
                .add(Property.forName(StoredUserPassword.Fields.userId).eq(userId));
    }

    private static UserPasswordAuthDetails toWire(final StoredUserPassword password) {
        return new UserPasswordAuthDetails(password.getUserId(),
                                           password.getPassword(),
                                           password.getFailedPasswordAttempts(),
                                           password.getCreated(),
                                           password.getUpdated());
    }

    @SneakyThrows
    private Optional<StoredUserPassword> saveInternal(
            String userId,
            StoredUserPassword updated) {
        return passwordDao.save(userId, updated);
    }

}
