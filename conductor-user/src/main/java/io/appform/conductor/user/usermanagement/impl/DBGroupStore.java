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
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredGroup;
import io.appform.conductor.server.usermanagement.impl.models.StoredGroupUserMapping;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Implementation for {@link Group} that stores information on a DB
 */
@Slf4j
public class DBGroupStore implements GroupStore {

    private final LookupDao<StoredGroup> groupDao;
    private final RelationalDao<StoredGroupUserMapping> groupUsersDao;

    @Inject
    public DBGroupStore(
            LookupDao<StoredGroup> groupDao,
            RelationalDao<StoredGroupUserMapping> groupUsersDao) {
        this.groupDao = groupDao;
        this.groupUsersDao = groupUsersDao;
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroup.GROUP_TABLE_NAME))
    public Optional<Group> create(@Throws.RuntimeParam("id") String name,
                                  String description,
                                  GroupType type,
                                  Set<String> requiredSkills) {
        val groupId = ConductorServerUtils.lowerSnake(name);
        return groupDao.createOrUpdate(groupId,
                                       g -> g.setDescription(description)
                                               .setType(type)
                                               .setRequiredSkills(requiredSkills)
                                               .setDeleted(false),
                                       () -> new StoredGroup(groupId, name, description, type, requiredSkills))
                .map(DBGroupStore::toWire);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroup.GROUP_TABLE_NAME))
    public Optional<Group> read(@Throws.RuntimeParam("id") String groupId) {
        return read(Collections.singletonList(groupId))
                .stream()
                .findAny();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroup.GROUP_TABLE_NAME))
    public List<Group> read(List<String> groupIds) {
        return groupDao.get(groupIds)
                .stream()
                .filter(g -> !g.isDeleted())
                .map(DBGroupStore::toWire)
                .toList();
    }

    @Override
    @MonitoredFunction
    public boolean delete(String groupId) {
        return groupDao.delete(groupId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroup.GROUP_TABLE_NAME))
    public Optional<Group> update(@Throws.RuntimeParam("id") String groupId, UnaryOperator<Group> handler) {
        boolean status = groupDao.update(groupId, groupOpt -> {
            val group = groupOpt.orElse(null);
            if (null != group) {
                val updated = handler.apply(toWire(group));
                group.setDescription(updated.getDescription())
                        .setType(updated.getType())
                        .setRequiredSkills(updated.getRequiredSkills())
                        .setDeleted(updated.isDeleted());
                return group;
            }
            return null;
        });
        log.info("Group {} update status: {}", groupId, status);
        return read(groupId);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroupUserMapping.GROUP_USERS_TABLE_NAME))
    public boolean addUserToGroup(@Throws.RuntimeParam("id") String groupId, String userId) {
        return groupUsersDao.createOrUpdate(userId,
                                            DetachedCriteria.forClass(StoredGroupUserMapping.class)
                                                    .add(Property.forName(StoredGroupUserMapping.Fields.groupId)
                                                                 .eq(groupId))
                                                    .add(Property.forName(StoredGroupUserMapping.Fields.userId)
                                                                 .eq(userId)),
                                            existing -> existing.setDeleted(false),
                                            () -> new StoredGroupUserMapping(groupId, userId))
                .isPresent();
    }


    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroupUserMapping.GROUP_USERS_TABLE_NAME))
    @SneakyThrows
    public boolean removeUserFromGroup(@Throws.RuntimeParam("id") String groupId, String userId) {
        return groupUsersDao.update(userId,
                                    DetachedCriteria.forClass(StoredGroupUserMapping.class)
                                            .add(Property.forName(StoredGroupUserMapping.Fields.groupId).eq(groupId))
                                            .add(Property.forName(StoredGroupUserMapping.Fields.userId).eq(userId)),
                                    existing -> existing.setDeleted(true));
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroupUserMapping.GROUP_USERS_TABLE_NAME))
    public List<String> findUsersForGroup(String groupId, int start, int limit) {
        return groupUsersDao.scatterGather(
                        DetachedCriteria.forClass(StoredGroupUserMapping.class)
                                .add(Property.forName(StoredGroupUserMapping.Fields.groupId).eq(groupId))
                                .add(Property.forName(StoredGroupUserMapping.Fields.deleted).eq(false)),
                        start,
                        limit)
                .stream()
                .map(StoredGroupUserMapping::getUserId)
                .toList();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredGroupUserMapping.GROUP_USERS_TABLE_NAME))
    @SneakyThrows
    public List<Group> findGroupsForUser(String userId) {
        return read(groupUsersDao.select(
                        userId,
                        DetachedCriteria.forClass(StoredGroupUserMapping.class)
                                .add(Property.forName(StoredGroupUserMapping.Fields.userId).eq(userId))
                                .add(Property.forName(StoredGroupUserMapping.Fields.deleted).eq(false)),
                        0,
                        Integer.MAX_VALUE)
                            .stream()
                            .map(StoredGroupUserMapping::getGroupId)
                            .toList());
    }

    @Override
    public List<Group> list() {
        return groupDao.scatterGather(DetachedCriteria.forClass(StoredGroup.class)
                                              .add(Property.forName(StoredGroup.Fields.deleted).eq(false)))
                .stream()
                .map(DBGroupStore::toWire)
                .toList();
    }

    private static Group toWire(@NonNull final StoredGroup group) {
        return new Group(
                group.getGroupId(),
                group.getName(),
                group.getDescription(),
                group.getType(),
                group.getRequiredSkills(),
                group.isDeleted(),
                group.getCreated(),
                group.getCreated());
    }
}
