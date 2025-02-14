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

import io.appform.conductor.model.tasks.TaskMode;
import io.appform.conductor.model.ticket.analytics.TicketGroupResponse;
import io.appform.conductor.model.ticket.analytics.TicketListResponse;
import io.appform.conductor.model.ticket.analytics.TicketQueryResponseVisitor;
import io.appform.conductor.server.eventmanagement.EventStore;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.taskmanagement.ConductorTaskScheduler;
import io.appform.conductor.model.tasks.RunActionOnCQLSelectTaskSpec;
import io.appform.conductor.model.tasks.Task;
import io.appform.conductor.model.tasks.TaskRunStatus;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class RunActionOnCQLSelectExecutor {
    private static final String TASK_META_CURSOR = "SCROLL_CURSOR";
    private final TicketManager ticketManager;
    private final EventStore eventStore;
    private final CQLEngine cqlEngine;

    @SuppressWarnings("unused")
    public ConductorTaskScheduler.TaskResult execute(
            final Task task,
            final Map<String, Object> taskMeta,
            final RunActionOnCQLSelectTaskSpec taskSpec) {
        val nextPtr = new AtomicReference<>((String) taskMeta.getOrDefault(TASK_META_CURSOR, ""));
        val hasMore = new AtomicBoolean(true);
        val parserOutput = cqlEngine.parse(taskSpec.getQuery()).orElse(null);
        if (null == parserOutput) {
            log.warn("Error parsing task CQL for task: {}, CQL: {}", task.getId(), taskSpec.getQuery());
            return new ConductorTaskScheduler.TaskResult(
                    TaskRunStatus.FAILURE,
                    task,
                    Map.of(TASK_META_CURSOR, nextPtr));
        }
        do {
            val queryResponse = CQLEngine.runQuery(
                    ConductorServerUtils.readableId(task.getId(), String.valueOf(System.currentTimeMillis())),
                    nextPtr.get(),
                    10,
                    parserOutput,
                    ticketManager,
                    eventStore);
            switch (queryResponse.getDomain()) {
                case TICKETS -> queryResponse.getTicketQueryResponse().accept(new TicketQueryResponseVisitor<Void>() {
                    @Override
                    public Void visit(TicketListResponse listResponse) {
                        hasMore.set(!listResponse.getResults().isEmpty());
                        nextPtr.set(listResponse.getNext());
                        listResponse.getResults()
                                .forEach(gist -> taskSpec.getActionIds()
                                        .forEach(actionId -> {
                                            log.info("Applying action {} on ticket {}",
                                                     actionId, gist.getTicketId());
                                            ticketManager.triggerTicketAction(gist.getTicketId(), actionId, null);
                                        }));
                        return null;
                    }

                    @Override
                    public Void visit(TicketGroupResponse groupResponse) {
                        log.warn("Aggregations are not supported on tasks as of now. Task ID: {}, CQL: {}",
                                 task.getId(),
                                 taskSpec.getQuery());
                        return null;
                    }
                });
                case EVENTS -> {
                    //TODO::EVENTS
                }
            }
        } while (hasMore.get());
        return new ConductorTaskScheduler.TaskResult(
                TaskRunStatus.SUCCESS,
                task,
                TaskMode.RESUMABLE == task.getMode() ? Map.of(TASK_META_CURSOR, nextPtr)
                        :  Map.of());
    }
}
