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

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.workflowmanagement.impl.DBWorkflowStore;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WorkflowManager}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.workflowmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class WorkflowManagerTest {

    @Test
    void testWorkflowCrud(BalancedDBShardingBundle<TestConfig> bundle) {
        val schemaStore = mock(SchemaStore.class);
        val actionStore = mock(ActionStore.class);
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
        when(schemaStore.get(anyString())).thenReturn(Optional.of(schema));
        val workflowStore = new DBWorkflowStore(bundle.createParentObjectDao(StoredWorkflow.class),
                                                bundle.createRelatedObjectDao(StoredTicketState.class),
                                                bundle.createRelatedObjectDao(StoredTicketStateTransition.class),
                                                bundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class));
        val wfm = new WorkflowManager(workflowStore, schemaStore, actionStore);

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

}