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
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUser;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * RDBMS backend for {@link UserStore}
 */
@Slf4j
public class DBUserStore implements UserStore {
    public static final String USER_TABLE_NAME = "users";

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
                                     .put("type", USER_TABLE_NAME)
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
                                     .put("type", USER_TABLE_NAME)
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
                                     .put("type", USER_TABLE_NAME)
                                     .build())
                    .cause(e)
                    .build();
        }    }

    @Override
    public Optional<UserSummary> getByEmail(String email) {
        try {
            return userDao.scatterGather(
                    DetachedCriteria.forClass(StoredUser.class)
                            .add(Property.forName("email").eq(email)))
                    .stream()
                    .findAny()
                    .map(DBUserStore::toWire);
        }
        catch(Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                            .put("type", USER_TABLE_NAME)
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
                            .put("type", USER_TABLE_NAME)
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
