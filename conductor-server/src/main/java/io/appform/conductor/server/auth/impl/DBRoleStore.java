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

import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.auth.Role;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.auth.RoleStore;
import io.appform.conductor.server.auth.impl.models.StoredRole;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static io.appform.conductor.model.error.ConductorErrorCode.*;

/**
 * DB implementation for {@link RoleStore}
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBRoleStore implements RoleStore {
    private final LookupDao<StoredRole> roleDao;

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRole.ROLES_TABLE_NAME))
    public Optional<Role> create(
            @Throws.RuntimeParam("id") String roleId,
            String name,
            String description,
            Set<Permission> permissions) {
        return roleDao.save(new StoredRole()
                                    .setRoleId(roleId)
                                    .setName(name)
                                    .setDescription(description)
                                    .setPermissions(permissions))
                .map(DBRoleStore::toRole);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRole.ROLES_TABLE_NAME))
    public Optional<Role> read(@Throws.RuntimeParam("id") String roleId) {
        return roleDao.get(roleId).map(DBRoleStore::toRole);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRole.ROLES_TABLE_NAME))
    public List<Role> list() {
        return roleDao.scatterGather(DetachedCriteria.forClass(StoredRole.class)
                                             .add(Property.forName(StoredRole.Fields.deleted).eq(false)))
                .stream()
                .map(DBRoleStore::toRole)
                .toList();
    }

    @Override
    public Set<Permission> permissionsForRoles(Collection<String> roleIds) {
        return roleDao.scatterGather(DetachedCriteria.forClass(StoredRole.class)
                                             .add(Property.forName(StoredRole.Fields.deleted).eq(false))
                                             .add(Restrictions.in(StoredRole.Fields.roleId, roleIds)))
                .stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRole.ROLES_TABLE_NAME))
    public Optional<Role> update(
            @Throws.RuntimeParam("id") String roleId,
            UnaryOperator<Role> handler) {
        roleDao.update(roleId,
                       storedRole -> storedRole.map(stored -> {
                                   val role = handler.apply(toRole(stored));
                                   return stored.setName(role.getName())
                                           .setDescription(role.getDescription())
                                           .setPermissions(role.getPermissions());
                               })
                               .orElse(null));
        return read(roleId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = STORE_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRole.ROLES_TABLE_NAME))
    public boolean delete(@Throws.RuntimeParam("id") String roleId) {
        return roleDao.update(roleId, storedRole -> storedRole.map(stored -> stored.setDeleted(true)).orElse(null));
    }

    private static Role toRole(StoredRole storedRole) {
        return new Role(storedRole.getRoleId(),
                        storedRole.getName(),
                        storedRole.getDescription(),
                        storedRole.getPermissions(),
                        storedRole.getCreated(),
                        storedRole.getUpdated());
    }
}
