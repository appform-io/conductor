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

package io.appform.conductor.server.skillmanagement.impl;

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.skills.SkillDefinition;
import io.appform.conductor.model.skills.SkillValue;
import io.appform.conductor.server.skillmanagement.SkillStore;
import io.appform.conductor.server.skillmanagement.impl.models.StoredSkillDefinition;
import io.appform.conductor.server.skillmanagement.impl.models.StoredSkillValue;
import io.appform.conductor.server.skillmanagement.impl.models.StoredUserSkillAssociation;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBSkillStore implements SkillStore {
    private final LookupDao<StoredSkillDefinition> skillDao;
    private final RelationalDao<StoredSkillValue> skillValueDao;
    private final RelationalDao<StoredUserSkillAssociation> userSkillDao;

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    @MonitoredFunction
    public Optional<SkillDefinition> createSkillDefinition(@Throws.RuntimeParam("id") String name) {
        val skillId = ConductorServerUtils.lowerSnake(name);
        return skillDao.createOrUpdate(skillId,
                                       existing -> existing.setName(name)
                                               .setDeleted(false),
                                       () -> new StoredSkillDefinition()
                                               .setSkillId(skillId)
                                               .setName(name))
                .flatMap(object -> readSkillDefinition(skillId));
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    @MonitoredFunction
    public Optional<SkillDefinition> updateSkillDefinition(
            @Throws.RuntimeParam("id") String skillId,
            String name) {
        return skillDao.update(skillId, skill -> skill.map(s -> s.setName(name)).orElse(null))
               ? readSkillDefinition(skillId)
               : Optional.empty();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillValue.SKILL_VALUE_TABLE_NAME))
    public Optional<SkillDefinition> addValueToSkillDefinition(
            @Throws.RuntimeParam("id") String id,
            @Throws.RuntimeParam("subId") String value) {
        val valueId = ConductorServerUtils.readableId(id, value);
        val updated = skillDao.lockAndGetExecutor(id)
                .createOrUpdate(skillValueDao,
                                DetachedCriteria.forClass(StoredSkillValue.class)
                                        .add(Property.forName(StoredSkillValue.Fields.skillId).eq(id))
                                        .add(Property.forName(StoredSkillValue.Fields.valueId).eq(valueId)),
                                existing -> existing.setValue(value)
                                        .setDeleted(false),
                                skill -> new StoredSkillValue()
                                        .setSkillId(id)
                                        .setValueId(valueId)
                                        .setValue(value))
                .execute();
        if (null != updated) {
            return readSkillDefinition(id);
        }
        return Optional.empty();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillValue.SKILL_VALUE_TABLE_NAME))
    public Optional<SkillDefinition> removeValueFromSkillDefinition(
            @Throws.RuntimeParam("id") String id,
            @Throws.RuntimeParam("subId") String valueId) {
        val updated = skillDao.lockAndGetExecutor(id)
                .update(skillValueDao,
                        DetachedCriteria.forClass(StoredSkillValue.class)
                                .add(Property.forName(StoredSkillValue.Fields.skillId).eq(id))
                                .add(Property.forName(StoredSkillValue.Fields.valueId).eq(valueId)),
                        existing -> existing.setDeleted(true),
                        () -> false)
                .execute();
        if (null != updated) {
            return readSkillDefinition(id);
        }
        return Optional.empty();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillValue.SKILL_VALUE_TABLE_NAME))
    public Optional<SkillDefinition> updateSkillValue(
            @Throws.RuntimeParam("id") String id,
            @Throws.RuntimeParam("subId") String valueId,
            String value) {
        val updated = skillDao.lockAndGetExecutor(id)
                .update(skillValueDao,
                        DetachedCriteria.forClass(StoredSkillValue.class)
                                .add(Property.forName(StoredSkillValue.Fields.skillId).eq(id))
                                .add(Property.forName(StoredSkillValue.Fields.valueId).eq(valueId)),
                        existing -> existing.setValue(value),
                        () -> false)
                .execute();
        if (null != updated) {
            return readSkillDefinition(id);
        }
        return Optional.empty();
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    @MonitoredFunction
    public Optional<SkillDefinition> readSkillDefinition(@Throws.RuntimeParam("id") String id) {
        return skillDao.readOnlyExecutor(id)
                .readAugmentParent(skillValueDao, DetachedCriteria.forClass(StoredSkillValue.class)
                                           .add(Property.forName(StoredSkillValue.Fields.skillId).eq(id))
                                           .add(Property.forName(StoredSkillValue.Fields.deleted).eq(false)),
                                   0,
                                   Integer.MAX_VALUE,
                                   StoredSkillDefinition::setValues)
                .execute()
                .filter(skill -> !skill.isDeleted())
                .map(DBSkillStore::toWire);
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillValue.SKILL_VALUE_TABLE_NAME))
    @MonitoredFunction
    public Optional<SkillValue> readSkillValue(
            @Throws.RuntimeParam("id") String id,
            @Throws.RuntimeParam("subId") String valueId) {
        return skillDao.readOnlyExecutor(id)
                .readAugmentParent(skillValueDao, DetachedCriteria.forClass(StoredSkillValue.class)
                                           .add(Property.forName(StoredSkillValue.Fields.deleted).eq(false))
                                           .add(Property.forName(StoredSkillValue.Fields.skillId).eq(id))
                                           .add(Property.forName(StoredSkillValue.Fields.valueId).eq(valueId)),
                                   0,
                                   Integer.MAX_VALUE,
                                   StoredSkillDefinition::setValues)
                .execute()
                .flatMap(skill -> skill.getValues()
                        .stream()
                        .findFirst()
                        .map(v -> new SkillValue(skill.getSkillId(),
                                                 v.getValueId(),
                                                 skill.getName(),
                                                 v.getValue(),
                                                 v.getCreated(),
                                                 v.getUpdated())));
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    @MonitoredFunction
    public boolean deleteSkillDefinition(@Throws.RuntimeParam("id") String skillId) {
        return skillDao.update(skillId,
                               existing -> existing.map(skill -> skill.setDeleted(true)).orElse(null));
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    public List<SkillDefinition> listSkillDefinitions() {
        return skillDao.scatterGather(DetachedCriteria.forClass(StoredSkillDefinition.class)
                                              .add(Property.forName(StoredSkillDefinition.Fields.deleted).eq(false)))
                .stream()
                .sorted(Comparator.comparing(StoredSkillDefinition::getName))
                .map(DBSkillStore::toWire)
                .toList();
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = {
            @Throws.Param(name = "type", value = StoredSkillValue.SKILL_VALUE_TABLE_NAME),
            @Throws.Param(name = "id", value = "all"),
            })
    public List<SkillValue> listSkillValues() {
        val values = skillValueDao.scatterGather(DetachedCriteria.forClass(StoredSkillValue.class)
                                                         .add(Property.forName(StoredSkillValue.Fields.deleted)
                                                                      .eq(false)),
                                                 0,
                                                 Integer.MAX_VALUE);
        val skillIds = skillDao.scatterGather(DetachedCriteria.forClass(StoredSkillDefinition.class)
                                                      .add(Property.forName(StoredSkillDefinition.Fields.deleted)
                                                                   .eq(false)))
                .stream()
                .collect(Collectors.toMap(StoredSkillDefinition::getSkillId, DBSkillStore::toWire));
        return values.stream()
                .filter(value -> skillIds.get(value.getSkillId()) != null)
                .map(value -> new SkillValue(value.getSkillId(),
                                             value.getValueId(),
                                             skillIds.get(value.getSkillId()).getName(),
                                             value.getValue(),
                                             value.getCreated(),
                                             value.getUpdated()
                ))
                .toList();
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserSkillAssociation.SKILL_ASSOCIATION_TABLE_NAME))
    @MonitoredFunction
    public boolean associateSkillWithUser(@Throws.RuntimeParam("id") String userId, String skillId, String valueId) {
        val skill = readSkillDefinition(skillId)
                .filter(skillDefinition -> skillDefinition.getValues()
                        .stream()
                        .anyMatch(value -> value.getSkillValueId().equals(valueId)))
                .orElse(null);
        if (null == skill) {
            return false;
        }
        val associationId = ConductorServerUtils.readableId(userId, skillId, valueId);
        return userSkillDao.createOrUpdate(userId,
                                           DetachedCriteria.forClass(StoredUserSkillAssociation.class)
                                                   .add(Property.forName(StoredUserSkillAssociation.Fields.associationId)
                                                                .eq(associationId)),
                                           existing -> existing.setDeleted(false),
                                           () -> new StoredUserSkillAssociation()
                                                   .setAssociationId(associationId)
                                                   .setUserId(userId)
                                                   .setSkillId(skillId)
                                                   .setValueId(valueId))
                .isPresent();
    }

    @SneakyThrows
    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredUserSkillAssociation.SKILL_ASSOCIATION_TABLE_NAME))
    public boolean disassociateSkillWithUser(@Throws.RuntimeParam("id") String userId, String skillId, String valueId) {
        val associationId = ConductorServerUtils.readableId(userId, skillId, valueId);
        return userSkillDao.update(userId,
                                   DetachedCriteria.forClass(StoredUserSkillAssociation.class)
                                           .add(Property.forName(StoredUserSkillAssociation.Fields.associationId)
                                                        .eq(associationId)),
                                   existing -> existing.setDeleted(true));
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    public List<SkillValue> listSkillsForUser(String userId) {
        return userSkillDao.select(userId,
                                   DetachedCriteria.forClass(StoredUserSkillAssociation.class)
                                           .add(Property.forName(StoredUserSkillAssociation.Fields.userId)
                                                        .eq(userId))
                                           .add(Property.forName(StoredUserSkillAssociation.Fields.deleted)
                                                        .eq(false)),
                                   0,
                                   Integer.MAX_VALUE)
                .stream()
                .map(data -> readSkillValue(data.getSkillId(), data.getValueId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private static SkillDefinition toWire(final StoredSkillDefinition storedSkillDefinition) {
        return new SkillDefinition(storedSkillDefinition.getSkillId(),
                                   storedSkillDefinition.getName(),
                                   Objects.requireNonNullElse(storedSkillDefinition.getValues(),
                                                              List.<StoredSkillValue>of())
                                           .stream()
                                           .filter(skillValue -> !skillValue.isDeleted())
                                           .map(storedSkillValue -> new SkillValue(
                                                   storedSkillValue.getSkillId(),
                                                   storedSkillValue.getValueId(),
                                                   storedSkillDefinition.getName(),
                                                   storedSkillValue.getValue(),
                                                   storedSkillValue.getCreated(),
                                                   storedSkillValue.getUpdated()))
                                           .collect(Collectors.toUnmodifiableSet()),
                                   storedSkillDefinition.getCreated(),
                                   storedSkillDefinition.getUpdated());
    }
}
