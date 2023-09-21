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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.taskmanagement.impl.DBTaskStore;
import io.appform.conductor.server.taskmanagement.impl.RunActionOnSelectedTicketsExecutor;
import io.appform.conductor.server.taskmanagement.impl.models.StoredTask;
import io.appform.conductor.server.taskmanagement.model.RunActionOnSelectedTicketsTaskSpec;
import io.appform.conductor.server.taskmanagement.model.Task;
import io.appform.conductor.server.taskmanagement.model.TaskState;
import io.appform.conductor.server.taskmanagement.model.TaskType;
import io.appform.conductor.server.ticketmanagement.TicketGist;
import io.appform.conductor.server.ticketmanagement.TicketGistListResult;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
@ExtendWith(DBTestExtension.class)
@RelevantDBEntityPackages("io.appform.conductor.server.taskmanagement.impl.models")
class ConductorTaskSchedulerTest {

    @Test
    @SneakyThrows
    void test(BalancedDBShardingBundle<TestConfig> bundle) {
        val tstore = new DBTaskStore(bundle.createParentObjectDao(StoredTask.class),
                                    new ObjectMapper());
        val tm = mock(TicketManager.class);
        val taskSpec = new RunActionOnSelectedTicketsTaskSpec(List.of(),
                                                              List.of(),
                                                              List.of());
        val callCounter = new AtomicInteger();

        val executor = mock(RunActionOnSelectedTicketsExecutor.class);
        when(executor.execute(any(Task.class), any(RunActionOnSelectedTicketsTaskSpec.class)))
                .thenAnswer(invocationOnMock -> {
                    callCounter.incrementAndGet();
                    return new ConductorTaskScheduler.TaskResult(ConductorTaskScheduler.TaskStatus.SUCCESS, Map.of());
                });
        val ticket = new TicketGist("t1", "Test", "TWF", "S1", false, TicketPriority.MEDIUM, new Date(), new Date());
        when(tm.since(anyList(), anyList(), anyString(), anyInt()))
                .thenReturn(TicketGistListResult.builder().results(List.of(ticket)).build());

        val scheduler = new ConductorTaskScheduler(executor, tstore);
        scheduler.start();
        scheduler.scheduleNewTask(new Task("TT",
                                           TaskType.RUN_ACTION_ON_SELECTED_TICKETS,
                                           "Test Task",
                                           "",
                                           Duration.ofSeconds(1),
                                           Scope.GLOBAL,
                                           TaskState.ACTIVE,
                                           taskSpec,
                                           null,
                                           Map.of(),
                                           null,
                                           null));
        await()
                .atMost(Duration.ofSeconds(6))
                .until(() -> callCounter.get() == 5)
                ;
        assertEquals(5, callCounter.get());
        val task = tstore.listByIds(List.of("TT")).stream().findFirst().orElse(null);
        assertNotNull(task);
        assertNotNull(task.getLastExecutionCompletionTime());
    }
}