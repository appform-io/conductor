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

package io.appform.conductor.server.auth.impl;

import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.auth.UserRoleMappingStore;
import io.appform.conductor.server.auth.impl.models.StoredUserRoleMapping;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_RELATED_ENTITY_LIST_ERROR;
import static io.appform.conductor.model.error.ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBUserRoleMappingStore implements UserRoleMappingStore {
    private final RelationalDao<StoredUserRoleMapping> userRolesDao;

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserRoleMapping.USER_ROLE_MAPPING_TABLE_NAME))
    public boolean assignRoleToUser(
            @Throws.RuntimeParam("id") String userId,
            @Throws.RuntimeParam("subId") String roleId) {
        return userRolesDao.save(userId, new StoredUserRoleMapping()
                        .setId(userId + "-" + roleId)
                        .setUserId(userId)
                        .setRoleId(roleId)
                        .setDeleted(false))
                .filter(mapping -> !mapping.isDeleted())
                .isPresent();
    }

    @Override
    @MonitoredFunction
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserRoleMapping.USER_ROLE_MAPPING_TABLE_NAME))
    public boolean revokeRoleFromUser(
            @Throws.RuntimeParam("id") String userId,
            @Throws.RuntimeParam("subId") String roleId) {
        return userRolesDao.update(userId,
                                   DetachedCriteria.forClass(StoredUserRoleMapping.class),
                                   mapping -> mapping.setDeleted(true));
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserRoleMapping.USER_ROLE_MAPPING_TABLE_NAME))
    public List<String> rolesForUser(@Throws.RuntimeParam("id") String userId) {
        return userRolesDao.select(userId,
                                   DetachedCriteria.forClass(StoredUserRoleMapping.class)
                                           .add(Property.forName(StoredUserRoleMapping.Fields.deleted).eq(false)),
                                   0,
                                   Integer.MAX_VALUE)
                .stream()
                .map(StoredUserRoleMapping::getRoleId)
                .toList();
    }
}
