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

package io.appform.conductor.server.taskmanagement;

import io.appform.conductor.server.taskmanagement.impl.RunActionOnSelectedTicketsExecutor;
import io.appform.conductor.server.taskmanagement.model.RunActionOnSelectedTicketsTaskSpec;
import io.appform.conductor.server.taskmanagement.model.Task;
import io.appform.conductor.server.taskmanagement.model.TaskSpecVisitor;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.kaal.KaalScheduler;
import io.appform.kaal.KaalTask;
import io.appform.kaal.KaalTaskData;
import io.appform.kaal.KaalTaskRunIdGenerator;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 *
 */
@Slf4j
@Singleton
public class ConductorTaskScheduler implements Managed {

    private final RunActionOnSelectedTicketsExecutor runActionOnSelectedTicketsExecutor;
    private final TaskStore taskStore;

    @Inject
    public ConductorTaskScheduler(
            RunActionOnSelectedTicketsExecutor runActionOnSelectedTicketsExecutor,
            TaskStore taskStore) {
        this.runActionOnSelectedTicketsExecutor = runActionOnSelectedTicketsExecutor;
        this.taskStore = taskStore;
        scheduler = KaalScheduler.<RunnableTask, TaskResult>builder()
                .withTaskIdGenerator(new IdentityIDGenerator())
                .build();
        scheduler.onTaskCompleted().connect(this::handleTaskResult);
    }

    @Override
    public void start() throws Exception {
        scheduler.start();
        taskStore.listByIds(List.of())
                .forEach(task -> {
                    scheduler.schedule(new RunnableTask(this, task));
                    log.info("Scheduled task: {}", task.getName());
                });
    }

    @Override
    public void stop() throws Exception {
        scheduler.clear();
        scheduler.stop();
    }

    public boolean scheduleNewTask(final Task task) {
        val taskId = ConductorServerUtils.lowerSnake(task.getName());
        return taskStore.createOrUpdate(taskId, task.withId(taskId))
                .map(savedTask -> {
                    val status = scheduler.schedule(new RunnableTask(this, task));
                    log.info("Scheduled task: {}", task.getName());
                    return status;
                })
                .isPresent();
    }

    public boolean updateTask(final String taskId, UnaryOperator<Task> updater) {
        return taskStore.update(taskId, updater)
                .map(updated -> {
                    scheduler.delete(taskId);
                    val status = scheduler.schedule(new RunnableTask(this, updated));
                    log.info("Replaced task: {}", status);
                    return status;
                })
                .isPresent();
    }

    public boolean deleteTask(final String taskId) {
        val status = taskStore.delete(taskId);
        if (status) {
            scheduler.delete(taskId);
            log.info("Deleted task: {}", taskId);
        }
        return status;
    }

    public enum TaskStatus {
        SUCCESS,
        FAILURE
    }

    public record TaskResult(TaskStatus status, Map<String, Object> taskMeta) {
    }

    public static final class RunnableTask implements KaalTask<RunnableTask, TaskResult> {
        private final ConductorTaskScheduler scheduler;
        private final Task task;

        public RunnableTask(ConductorTaskScheduler scheduler, Task task) {
            this.scheduler = scheduler;
            this.task = task;
        }

        @Override
        public String id() {
            return task.getId();
        }

        @Override
        public long delayToNextRun(Date currentTime) {
            return task.getInterval().toMillis();
        }

        @Override
        public TaskResult apply(Date date, KaalTaskData<RunnableTask, TaskResult> runnable) {
            return runnable.getTask()
                    .task.getSpec()
                    .accept(new TaskSpecVisitor<TaskResult>() {
                        @Override
                        public TaskResult visit(RunActionOnSelectedTicketsTaskSpec runActionOnSelectedTicketsTaskSpec) {
                            return scheduler.runActionOnSelectedTicketsExecutor
                                    .execute(runnable.getTask().task, runActionOnSelectedTicketsTaskSpec);
                        }
                    });
        }
    }

    private static final class IdentityIDGenerator implements KaalTaskRunIdGenerator<RunnableTask, TaskResult> {

        @Override
        public String generateId(RunnableTask task, Date executionTime) {
            return task.id() + "-" + executionTime.getTime();
        }
    }

    private final KaalScheduler<RunnableTask, TaskResult> scheduler;

    private void handleTaskResult(final KaalTaskData<RunnableTask, TaskResult> result) {
        val task = result.getTask();
        val conductorTask = task.task;
        val exception = result.getException();
        if (exception != null) {
            log.error("Error in task run ", result.getRunId() + ": " + exception.getMessage(), exception);
            //TODO::EVENT
        }
        val updated = taskStore.createOrUpdate(conductorTask.getId(),
                                               conductorTask.withLastExecutionCompletionTime(new Date())
                                                       .withTaskMeta(result.getResult().taskMeta()))
                .orElse(null);
        log.debug("Saved task for: {}", conductorTask.getId());
    }
}
