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

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    @MonitoredFunction
    public Optional<SkillDefinition> createSkill(@Throws.RuntimeParam("id") String name) {
        val skillId = ConductorServerUtils.lowerSnake(name);
        return skillDao.createOrUpdate(skillId,
                                       existing -> existing.setName(name)
                                               .setDeleted(false),
                                       () -> new StoredSkillDefinition()
                                               .setSkillId(skillId)
                                               .setName(name))
                .flatMap(object -> readSkill(skillId));
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillValue.SKILL_VALUE_TABLE_NAME))
    public Optional<SkillDefinition> addValueToSkill(
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
                                skill -> {
                                    return new StoredSkillValue()
                                            .setSkillId(id)
                                            .setValueId(valueId)
                                            .setValue(value);
                                })
                .execute();
        if (null != updated) {
            return readSkill(id);
        }
        return Optional.empty();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillValue.SKILL_VALUE_TABLE_NAME))
    public Optional<SkillDefinition> removeValueFromSkill(
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
            return readSkill(id);
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
            return readSkill(id);
        }
        return Optional.empty();
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    @MonitoredFunction
    public Optional<SkillDefinition> readSkill(@Throws.RuntimeParam("id") String id) {
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
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    @MonitoredFunction
    public boolean deleteSkill(@Throws.RuntimeParam("id") String skillId) {
        return skillDao.update(skillId,
                               existing -> existing.map(skill -> skill.setDeleted(true)).orElse(null));
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME))
    public List<SkillDefinition> list() {
        return skillDao.scatterGather(DetachedCriteria.forClass(StoredSkillDefinition.class)
                                              .add(Property.forName(StoredSkillDefinition.Fields.deleted).eq(false)))
                .stream()
                .sorted(Comparator.comparing(StoredSkillDefinition::getName))
                .map(DBSkillStore::toWire)
                .toList();
    }

    private static SkillDefinition toWire(final StoredSkillDefinition storedSkillDefinition) {
        return new SkillDefinition(storedSkillDefinition.getSkillId(),
                                   storedSkillDefinition.getName(),
                                   Objects.requireNonNullElse(storedSkillDefinition.getValues(),
                                                              List.<StoredSkillValue>of())
                                           .stream()
                                           .filter(skillValue -> !skillValue.isDeleted())
                                           .map(storedSkillValue -> new SkillValue(storedSkillValue.getValueId(),
                                                                                   storedSkillDefinition.getName(),
                                                                                   storedSkillValue.getValue(),
                                                                                   storedSkillValue.getCreated(),
                                                                                   storedSkillValue.getUpdated()))
                                           .collect(Collectors.toUnmodifiableSet()),
                                   storedSkillDefinition.getCreated(),
                                   storedSkillDefinition.getUpdated());
    }
}
