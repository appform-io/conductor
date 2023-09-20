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

import io.appform.conductor.server.taskmanagement.model.RunActionOnSelectedTicketsTaskSpec;
import io.appform.conductor.server.taskmanagement.model.Task;
import io.appform.conductor.server.taskmanagement.model.TaskSpecVisitor;
import io.appform.kaal.KaalScheduler;
import io.appform.kaal.KaalTask;
import io.appform.kaal.KaalTaskData;
import io.dropwizard.lifecycle.Managed;

import java.util.Date;

/**
 *
 */
public class ConductorTaskScheduler implements Managed {
    public ConductorTaskScheduler() {
        scheduler = KaalScheduler.<RunnableTask, TaskResult>builder().build();
//        scheduler.onTaskCompleted().connect()
    }

    @Override
    public void start() throws Exception {
        scheduler.start();
    }

    @Override
    public void stop() throws Exception {
        scheduler.clear();
        scheduler.stop();
    }

    public enum TaskStatus {
        SUCCESS,
        FAILURE
    }

    public record TaskResult(TaskStatus status) {
    }

    public static final class RunnableTask implements KaalTask<RunnableTask, TaskResult> {
        private final Task task;

        public RunnableTask(Task task) {
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
                            return new TaskResult(TaskStatus.SUCCESS);
                        }
                    });
        }
    }

    private final KaalScheduler<RunnableTask, TaskResult> scheduler;

    private void handleTaskResult(final KaalTaskData<RunnableTask, TaskResult> result) {

    }
}
