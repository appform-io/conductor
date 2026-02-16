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

package io.appform.conductor.server.workflowmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.core.workflowmanagement.WorkflowManager;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.workflow.ImportResult;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.model.workflow.WorkflowDetails;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.core.actionmanagement.ActionStore;
import io.appform.conductor.core.actionmanagement.impl.DBActionStore;
import io.appform.conductor.core.actionmanagement.impl.models.StoredAction;
import io.appform.conductor.core.schemamanagement.impl.DBSchemaStore;
import io.appform.conductor.core.schemamanagement.impl.SchemaStore;
import io.appform.conductor.core.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.core.schemamanagement.impl.models.StoredSchemaSummary;
import io.appform.conductor.core.taskmanagement.ConductorTaskScheduler;
import io.appform.conductor.core.taskmanagement.TaskStore;
import io.appform.conductor.core.taskmanagement.impl.DBTaskStore;
import io.appform.conductor.core.taskmanagement.impl.RunActionOnCQLSelectExecutor;
import io.appform.conductor.core.taskmanagement.impl.RunActionOnSelectedTicketsExecutor;
import io.appform.conductor.core.taskmanagement.impl.models.StoredTask;
import io.appform.conductor.core.ticketmanagement.TicketStore;
import io.appform.conductor.core.ticketmanagement.impl.DBTicketStore;
import io.appform.conductor.core.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.core.utils.ConductorServerUtils;
import io.appform.conductor.core.workflowmanagement.impl.DBWorkflowStore;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.dropwizard.util.Resources;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link io.appform.conductor.core.workflowmanagement.WorkflowManager}
 */
@RelevantDBEntityPackages({"io.appform.conductor.server.workflowmanagement.impl.models",
        "io.appform.conductor.server.schemamanagement.impl.models",
        "io.appform.conductor.server.actionmanagement.impl.models",
        "io.appform.conductor.server.ticketmanagement.impl.models",
        "io.appform.conductor.server.taskmanagement.impl.models",
})
@ExtendWith(DBTestExtension.class)
class WorkflowManagerTest {

    @Test
    void testWorkflowCrud(BalancedDBShardingBundle<TestConfig> bundle) {
        val schemaStore = mock(SchemaStore.class);
        val actionStore = mock(ActionStore.class);
        val ticketStore = mock(TicketStore.class);
        val taskStore = mock(TaskStore.class);
        val taskScheduler = mock(ConductorTaskScheduler.class);
        val schema = new Schema("S1",
                                "S1",
                                null,
                                1,
                                SchemaState.ACTIVE,
                                null,
                                null,
                                null,
                                null,
                                null);
        when(schemaStore.read(anyString())).thenReturn(Optional.of(schema));
        val workflowStore = new DBWorkflowStore(bundle.createParentObjectDao(StoredWorkflow.class),
                                                bundle.createRelatedObjectDao(StoredTicketState.class),
                                                bundle.createRelatedObjectDao(StoredTicketStateTransition.class),
                                                bundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class));
        val wfm = new WorkflowManager(workflowStore, schemaStore, actionStore, taskStore, taskScheduler, ticketStore);

        val wf = wfm.create("Test",
                            "Test workflow",
                            "S1",
                            Template.fixed("Test ticket"),
                            Template.fixed("Test description"),
                            Template.fixed("{}")).orElse(null);
        assertNotNull(wf);
        try {
            wfm.create("Test",
                       "Test workflow",
                       "S1",
                       Template.fixed("Test ticket"),
                       Template.fixed("Test description"),
                       Template.fixed("{}"));
            fail("Should have thrown as it's a duplicate entry");
        }
        catch (ConductorException e) {
            assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, e.getErrorCode());
        }
    }

    @Test
    void testWorkflowExport(BalancedDBShardingBundle<TestConfig> bundle) throws Exception {
        val mapper = new ObjectMapper();
        ConductorServerUtils.configureMapper(mapper);
        val workflowDetails = mapper.readValue(fixture("fixtures/workflow_details.json"),
                                               WorkflowDetails.class);
        val workflowStore = new DBWorkflowStore(bundle.createParentObjectDao(StoredWorkflow.class),
                                                bundle.createRelatedObjectDao(StoredTicketState.class),
                                                bundle.createRelatedObjectDao(StoredTicketStateTransition.class),
                                                bundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class));
        val schemaStore = new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                            bundle.createRelatedObjectDao(StoredFieldSchema.class), mapper);
        val actionStore = new DBActionStore(bundle.createParentObjectDao(StoredAction.class));
        val ticketStore = new DBTicketStore(bundle.createParentObjectDao(StoredTicketSkeleton.class), null,
                                            null, null, null, mapper);
        val taskStore = new DBTaskStore(bundle.createParentObjectDao(StoredTask.class), mapper);
        val scheduler = new ConductorTaskScheduler(
                mock(RunActionOnSelectedTicketsExecutor.class),
                mock(RunActionOnCQLSelectExecutor.class),
                taskStore);
        val wfm = new WorkflowManager(workflowStore, schemaStore, actionStore, taskStore, scheduler, ticketStore);
        val result = wfm.importWorkflow(workflowDetails, false, false);
        assertTrue(result.getWorkflow().isSuccess());
        assertTrue(result.getSchema().isSuccess());
        assertFalse(result.getActions().stream()
                            .anyMatch(actionImportResult -> !actionImportResult.isSuccess()));
        assertFalse(result.getActions().stream()
                            .anyMatch(actionImportResult -> !actionImportResult.isSuccess()));

        {
            val idempotentResult = wfm.importWorkflow(workflowDetails, false, false);
            assertTrue(idempotentResult.getWorkflow().isSuccess());
            assertTrue(idempotentResult.getSchema().isSuccess());
            assertTrue(idempotentResult.getActions().stream()
                                .noneMatch(ImportResult::isSuccess)); //Actions can't be overwritten
            assertTrue(idempotentResult.getTasks().stream()
                                .noneMatch(ImportResult::isSuccess)); //Tasks can't be overwritten
        }
        {
            val idempotentResult = wfm.importWorkflow(workflowDetails, true, true);
            assertTrue(idempotentResult.getWorkflow().isSuccess());
            assertTrue(idempotentResult.getSchema().isSuccess());
            assertTrue(idempotentResult.getActions().stream()
                                .allMatch(ImportResult::isSuccess)); //Actions overwritten
            assertTrue(idempotentResult.getTasks().stream()
                                .allMatch(ImportResult::isSuccess)); //Tasks overwritten
        }
    }


    private String fixture(String filename) {
        URL resource = Resources.getResource(filename);
        try {
            return Resources.toString(resource, StandardCharsets.UTF_8).trim();
        }
        catch (IOException var4) {
            throw new IllegalArgumentException(var4);
        }
    }
}