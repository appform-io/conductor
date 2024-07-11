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

package io.appform.conductor.server.taskmanagement.impl;

import io.appform.conductor.server.taskmanagement.ConductorTaskScheduler;
import io.appform.conductor.model.tasks.RunActionOnSelectedTicketsTaskSpec;
import io.appform.conductor.model.tasks.Task;
import io.appform.conductor.model.tasks.TaskRunStatus;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class RunActionOnSelectedTicketsExecutor {
    private static final String TASK_META_CURSOR = "SCROLL_CURSOR";
    private final TicketManager ticketManager;

    @SuppressWarnings("unused")
    public ConductorTaskScheduler.TaskResult execute(
            final Task task,
            final Map<String, Object> taskMeta,
            final RunActionOnSelectedTicketsTaskSpec taskSpec) {
        var nextPtr = (String) taskMeta.getOrDefault(TASK_META_CURSOR, "");
        var hasMore = true;
        do {
            val tickets = ticketManager.since(taskSpec.getTicketFilters(),
                                              taskSpec.getFieldFilters(),
                                              nextPtr,
                                              10);
            hasMore = !tickets.getResults().isEmpty();
            nextPtr = tickets.getNext();
            tickets.getResults()
                    .forEach(gist -> taskSpec.getActionIds()
                            .forEach(actionId -> {
                                log.info("Applying action {} on ticket {}",
                                        actionId, gist.getTicketId());
                                ticketManager.triggerTicketAction(gist.getTicketId(), actionId);
                            }));
        } while (hasMore);
        return new ConductorTaskScheduler.TaskResult(
                TaskRunStatus.SUCCESS,
                task,
                Map.of(TASK_META_CURSOR, nextPtr));
    }
}
