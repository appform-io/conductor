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

import com.google.common.base.Strings;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUser;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.appform.conductor.server.usermanagement.impl.models.StoredUser.USER_TABLE_NAME;

/**
 * RDBMS backend for {@link UserStore}
 */
@Slf4j
public class DBUserStore implements UserStore {

    private final LookupDao<StoredUser> userDao;

    @Inject
    public DBUserStore(LookupDao<StoredUser> userDao) {
        this.userDao = userDao;
    }

    @Override
    @SneakyThrows
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = USER_TABLE_NAME))
    public Optional<UserSummary> create(
            @Throws.RuntimeParam("id") String userId, String name, UserType userType, String email) {
        return userDao.save(
                        new StoredUser(
                                userId,
                                userType,
                                name,
                                email,
                                UserState.CREATED))
                .map(DBUserStore::toWire);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = USER_TABLE_NAME))
    public Optional<UserSummary> getById(@Throws.RuntimeParam("id") String userId) {
        return Strings.isNullOrEmpty(userId)
               ? Optional.empty()
               : userDao.get(userId).map(DBUserStore::toWire);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = USER_TABLE_NAME))
    public List<UserSummary> getByIds(List<String> userIds) {
        return userDao.get(userIds)
                .stream()
                .map(DBUserStore::toWire)
                .toList();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_QUERY_ERROR,
            fixedParams = {
                    @Throws.Param(name = "type", value = USER_TABLE_NAME),
                    @Throws.Param(name = "paramType", value = "email"),
            })
    public Optional<UserSummary> getByEmail(@Throws.RuntimeParam("value") String email) {
        return userDao.scatterGather( //TODO::Move to shard query as userId & email has same shard
                        DetachedCriteria.forClass(StoredUser.class)
                                .add(Property.forName(StoredUser.Fields.email).eq(email)))
                .stream()
                .findAny()
                .map(DBUserStore::toWire);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = USER_TABLE_NAME))
    public Optional<UserSummary> update(
            @Throws.RuntimeParam("id") String userId, UnaryOperator<UserSummary> handler) {
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
