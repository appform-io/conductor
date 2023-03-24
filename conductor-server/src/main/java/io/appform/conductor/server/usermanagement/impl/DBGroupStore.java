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
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredGroup;
import io.appform.conductor.server.usermanagement.impl.models.StoredGroupUserMapping;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Implementation for {@link Group} that stores information on a DB
 */
@Slf4j
public class DBGroupStore implements GroupStore {

    public static final String GROUP_TABLE_NAME = "groups";
    public static final String GROUP_USERS_TABLE_NAME = "group_users";

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
    public Optional<Group> create(String name, String description) {
        final String groupId = ConductorServerUtils.normalize(name);
        try {
            return groupDao.save(new StoredGroup(groupId, name, description))
                    .map(DBGroupStore::toWire);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", GROUP_TABLE_NAME)
                                     .put("id", groupId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Optional<Group> get(String groupId) {
        return get(Collections.singletonList(groupId))
                .stream()
                .findAny();
    }

    @Override
    public List<Group> get(List<String> groupIds) {
        try {
            return groupDao.get(groupIds)
                    .stream()
                    .map(DBGroupStore::toWire)
                    .toList();
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", GROUP_TABLE_NAME)
                                     .put("id", groupIds.toString())
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Optional<Group> delete(String groupId) {
        return update(groupId, groupDetails -> groupDetails.setDeleted(true));
    }

    @Override
    public Optional<Group> update(
            String groupId, Consumer<Group> handler) {
        try {
            boolean status = groupDao.update(groupId, groupOpt -> {
                val group = groupOpt.orElse(null);
                if (null != group) {
                    val groupDetails = toWire(group);
                    handler.accept(groupDetails);
                    group.setDescription(groupDetails.getDescription())
                            .setDeleted(groupDetails.isDeleted());
                    return group;
                }
                return null;
            });
            log.info("Group {} update status: {}", groupId, status);
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", GROUP_TABLE_NAME)
                                     .put("id", groupId)
                                     .build())
                    .cause(e)
                    .build();
        }
        return get(groupId);
    }

    @Override
    public boolean addUserToGroup(String groupId, String userId) {
        try {
            return groupUsersDao.save(groupId, new StoredGroupUserMapping(groupId, userId))
                    .isPresent();
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", GROUP_TABLE_NAME)
                                     .put("id", groupId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public boolean removeUserFromGroup(String groupId, String userId) {
        try {
            return groupUsersDao.update(groupId,
                                        DetachedCriteria.forClass(StoredGroupUserMapping.class)
                                                .add(Property.forName("groupId").eq(groupId))
                                                .add(Property.forName("userId").eq(userId)),
                                        storedGroupUserMapping -> {
                                            storedGroupUserMapping.setDeleted(true);
                                            return storedGroupUserMapping;
                                        });
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_WRITE_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", GROUP_TABLE_NAME)
                                     .put("id", groupId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public List<String> findUsersForGroup(String groupId, int start, int limit) {
        try {
            return groupUsersDao.select(
                            groupId,
                            DetachedCriteria.forClass(StoredGroupUserMapping.class)
                                    .add(Property.forName("groupId").eq(groupId)),
                            start,
                            limit)
                    .stream()
                    .map(StoredGroupUserMapping::getUserId)
                    .toList();
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_READ_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", GROUP_TABLE_NAME)
                                     .put("id", groupId)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    @Override
    public List<Group> findGroupsForUser(String userId) {
        try {
            return get(groupUsersDao.scatterGather(
                            DetachedCriteria.forClass(StoredGroupUserMapping.class)
                                    .add(Property.forName("userId").eq(userId)),
                            0,
                            Integer.MAX_VALUE)
                               .stream()
                               .map(StoredGroupUserMapping::getUserId)
                               .toList());
        }
        catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.STORE_LIST_ERROR)
                    .context(ImmutableMap.<String, Object>builder()
                                     .put("type", GROUP_TABLE_NAME)
                                     .build())
                    .cause(e)
                    .build();
        }
    }

    private static Group toWire(@NonNull final StoredGroup group) {
        return new Group(
                group.getGroupId(),
                group.getName(),
                group.getDescription(),
                group.isDeleted(),
                group.getCreated(),
                group.getCreated());
    }
}
