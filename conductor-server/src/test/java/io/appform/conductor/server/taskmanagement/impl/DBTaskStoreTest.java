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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.taskmanagement.impl.models.StoredTask;
import io.appform.conductor.server.taskmanagement.model.RunActionOnSelectedTicketsTaskSpec;
import io.appform.conductor.server.taskmanagement.model.Task;
import io.appform.conductor.server.taskmanagement.model.TaskState;
import io.appform.conductor.server.taskmanagement.model.TaskType;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.taskmanagement.impl.models")

@ExtendWith(DBTestExtension.class)
class DBTaskStoreTest {
    @Test
    void testCreate(BalancedDBShardingBundle<TestConfig> bundle) {
        val store = new DBTaskStore(bundle.createParentObjectDao(StoredTask.class),
                                    new ObjectMapper());
        val saved = store.createOrUpdate("T1",
                                         new Task("T1",
                                                  TaskType.RUN_ACTION_ON_SELECTED_TICKETS,
                                                  "Test",
                                                  "Blah",
                                                  Duration.ofMinutes(1),
                                                  Scope.GLOBAL,
                                                  TaskState.ACTIVE,
                                                  new RunActionOnSelectedTicketsTaskSpec(List.of(),
                                                                                         List.of(),
                                                                                         List.of()),
                                                  new Date(),
                                                  new Date(),
                                                  new Date()));
        assertTrue(saved.isPresent());
        assertEquals("T1", saved.map(Task::getId).orElse(null));
        assertNotNull(saved.map(Task::getSpec).orElse(null));
    }
}