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

package io.appform.conductor.core.taskmanagement.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.core.taskmanagement.TaskStore;
import io.appform.conductor.core.taskmanagement.impl.models.StoredTask;
import io.appform.conductor.model.tasks.Task;
import io.appform.conductor.model.tasks.TaskSpec;
import io.appform.conductor.core.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DBTaskStore implements TaskStore {
    private final LookupDao<StoredTask> taskDao;
    private final ObjectMapper mapper;

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTask.TASK_TABLE_NAME))
    public Optional<Task> createOrUpdate(String id, @Throws.RuntimeParam("id") Task task) {
        return taskDao.createOrUpdate(
                        task.getId(),
                        existing -> copyAttributes(existing, task)
                                .setDeleted(false),
                        () -> toStored(task))
                .map(this::toWire);
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTask.TASK_TABLE_NAME))
    public Optional<Task> update(@Throws.RuntimeParam("id") String id, UnaryOperator<Task> updater) {
        return taskDao.update(id,
                              existing -> existing.map(stored -> copyAttributes(stored, updater.apply(toWire(stored))))
                                      .orElse(null))
               ? listByIds(List.of(id)).stream().findAny()
               : Optional.empty();
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTask.TASK_TABLE_NAME))
    public boolean delete(@Throws.RuntimeParam("id") String id) {
        return taskDao.update(id,
                              existing -> existing.map(stored -> stored.setDeleted(true)).orElse(null));
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTask.TASK_TABLE_NAME))
    public Optional<Task> read(@Throws.RuntimeParam("id") String taskId) {
        return taskDao.get(taskId).map(this::toWire);
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTask.TASK_TABLE_NAME))
    public List<Task> listByIds(List<String> ids) {
        val criteria = DetachedCriteria.forClass(StoredTask.class)
                .add(Property.forName(StoredTask.Fields.deleted).eq(false));
        if (null != ids && !ids.isEmpty()) {
            criteria.add(Property.forName(StoredTask.Fields.taskId).in(ids));
        }
        return list(criteria);
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTask.TASK_TABLE_NAME))
    public List<Task> listByScopes(List<Scope> scopes) {
        val criteria = DetachedCriteria.forClass(StoredTask.class)
                .add(Property.forName(StoredTask.Fields.deleted).eq(false));
        if (null != scopes && !scopes.isEmpty()) {
            val scopeChain = Restrictions.or();
            scopes.forEach(scope -> scopeChain.add(Restrictions.and(
                    Property.forName(StoredTask.Fields.scopeType).eq(scope.getType()),
                    Property.forName(StoredTask.Fields.scopeReferenceId).eq(scope.getReferenceId()))));
            criteria.add(scopeChain);
        }
        return list(criteria);
    }

    private List<Task> list(DetachedCriteria criteria) {
        return taskDao.scatterGather(criteria)
                .stream()
                .map(this::toWire)
                .toList();
    }

    public StoredTask toStored(final Task task) {
        return copyAttributes(new StoredTask(), task);
    }

    @SneakyThrows
    public StoredTask copyAttributes(final StoredTask storedTask, final Task task) {
        ConductorServerUtils.validateCron(task.getId(), task.getCron());
        return storedTask
                .setTaskId(task.getId())
                .setType(task.getType())
                .setName(task.getName())
                .setDescription(task.getDescription())
                .setCron(task.getCron())
                .setScopeType(task.getScope().getType())
                .setScopeReferenceId(task.getScope().getReferenceId())
                .setState(task.getState())
                .setMode(task.getMode())
                .setSpec(mapper.writeValueAsString(task.getSpec()))
                .setLastExecutionCompletionTime(task.getLastExecutionCompletionTime())
                .setLastRunStatus(task.getLastRunStatus())
                .setTaskMeta(mapper.writeValueAsString(task.getTaskMeta()));
    }

    @SneakyThrows
    public Task toWire(final StoredTask task) {
        return new Task(
                task.getTaskId(),
                task.getType(),
                task.getName(),
                task.getDescription(),
                task.getCron(),
                Scope.create(task.getScopeType(), task.getScopeReferenceId()),
                task.getState(),
                task.getMode(),
                mapper.readValue(task.getSpec(), TaskSpec.class),
                task.getLastExecutionCompletionTime(),
                task.getLastRunStatus(),
                mapper.readValue(task.getTaskMeta(), new TypeReference<>() {
                }),
                task.getCreated(),
                task.getUpdated()
        );
    }
}
