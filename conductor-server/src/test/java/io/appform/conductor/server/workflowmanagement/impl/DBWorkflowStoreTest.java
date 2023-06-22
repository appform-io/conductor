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

package io.appform.conductor.server.workflowmanagement.impl;

import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.server.DBTestBase;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class DBWorkflowStoreTest extends DBTestBase {

    @Test
    void testBasicCrud() {
        val ds = new DBWorkflowStore(
                bundle.createParentObjectDao(StoredWorkflow.class),
                bundle.createRelatedObjectDao(StoredTicketState.class),
                bundle.createRelatedObjectDao(StoredTicketStateTransition.class),
                bundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class));
        val original = ds.create("TEST_WF", "Test Workflow", "For testing", "RandomSchema")
                .orElse(null);
        assertNotNull(original);
        {
            val updated = ds.addState(original.getId(), "START", "Start State", "For testing", false)
                    .orElse(null);
            assertEquals(1, updated.getStates().size());
        }
        {
            val updated = ds.addState(original.getId(), "INTERMEDIATE_1", "Intermediate State", "For testing", false)
                    .orElse(null);
            assertEquals(2, updated.getStates().size());
        }        {
            val updated = ds.addState(original.getId(), "INTERMEDIATE_2", "Intermediate State", "For testing", false)
                    .orElse(null);
            assertEquals(3, updated.getStates().size());
        }
        {
            val updated = ds.addState(original.getId(), "END", "End State", "For testing", true)
                    .orElse(null);
            assertEquals(4, updated.getStates().size());
        }
        {
            val updated = ds.addTransition(original.getId(),
                                           "S_I1",
                                           "START",
                                           "INTERMEDIATE_1",
                                           TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                           new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                           "NO_ACTION")
                    .orElse(null);
            assertEquals(1, updated.getTicketStateTransitions().size());
            assertEquals(1, updated.getTicketStateTransitions().get("START").size());
        }
        {
            val updated = ds.addTransition(original.getId(),
                                           "S_I2",
                                           "START",
                                           "INTERMEDIATE_2",
                                           TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                           new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                           "NO_ACTION")
                    .orElse(null);
            assertEquals(1, updated.getTicketStateTransitions().size());
            assertEquals(2, updated.getTicketStateTransitions().get("START").size());
        }
        {
            val updated = ds.addTransition(original.getId(),
                                           "I1_E",
                                           "INTERMEDIATE_1",
                                           "END",
                                           TicketStateTransition.TicketStateTransitionType.DEFAULT,
                                           new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                           "NO_ACTION")
                    .orElse(null);
            assertEquals(2, updated.getTicketStateTransitions().size());
            assertEquals(1, updated.getTicketStateTransitions().get("INTERMEDIATE_1").size());
        }
        {
            val updated = ds.addTransition(original.getId(),
                                           "I2_E",
                                           "INTERMEDIATE_2",
                                           "END",
                                           TicketStateTransition.TicketStateTransitionType.DEFAULT,
                                           new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                           "NO_ACTION")
                    .orElse(null);
            assertEquals(3, updated.getTicketStateTransitions().size());
            assertEquals(1, updated.getTicketStateTransitions().get("INTERMEDIATE_2").size());
        }
        {
            val updated = ds.addSelectionRule(original.getId(),
                                           "R1",
                                           new Rule(Rule.RuleType.JSON_RULE, "{}"))
                    .orElse(null);
            assertEquals(1, updated.getRules().size());
            assertNotNull(updated.getRules().get("R1"));
        }
        {
            val updated = ds.addSelectionRule(original.getId(),
                                           "R2",
                                           new Rule(Rule.RuleType.JSON_RULE, "{}"))
                    .orElse(null);
            assertEquals(2, updated.getRules().size());
            assertNotNull(updated.getRules().get("R2"));
        }
    }

}