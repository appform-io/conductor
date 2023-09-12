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
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link DBWorkflowStore}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.workflowmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBWorkflowStoreTest {

    @Test
    void testBasicCrud(BalancedDBShardingBundle<TestConfig> bundle) {
        val ds = new DBWorkflowStore(
                bundle.createParentObjectDao(StoredWorkflow.class),
                bundle.createRelatedObjectDao(StoredTicketState.class),
                bundle.createRelatedObjectDao(StoredTicketStateTransition.class),
                bundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class));
        val original = ds.create("TEST_WF",
                                 "Test Workflow",
                                 "For testing",
                                 "RandomSchema",
                                 Template.fixed("Test ticket"),
                                 Template.fixed("Test description"),
                                 Template.fixed("{}"))
                .orElse(null);
        assertNotNull(original);
        {
            val updated = ds.createOrUpdateState(original.getId(),
                                                 "START",
                                                 "Start State",
                                                 "For testing",
                                                 false,
                                                 List.of(),
                                                 List.of(),
                                                 List.of("F1", "F2"),
                                                 List.of("F1"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(1, updated.getStates().size());
        }
        {
            val updated = ds.createOrUpdateState(original.getId(),
                                                 "INTERMEDIATE_1",
                                                 "Intermediate State",
                                                 "For testing",
                                                 false,
                                                 List.of("A1", "A2"),
                                                 List.of("F1"),
                                                 List.of("F1", "F2"),
                                                 List.of("F1"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(2, updated.getStates().size());
        } {
            val updated = ds.createOrUpdateState(original.getId(),
                                                 "INTERMEDIATE_2",
                                                 "Intermediate State",
                                                 "For testing",
                                                 false,
                                                 List.of("A3"),
                                                 List.of("F2"),
                                                 List.of("F1", "F2"),
                                                 List.of("F1"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(3, updated.getStates().size());
        }
        {
            val updated = ds.createOrUpdateState(original.getId(),
                                                 "END",
                                                 "End State",
                                                 "For testing",
                                                 true,
                                                 List.of(),
                                                 List.of(),
                                                 List.of("F1", "F2"),
                                                 List.of("F1"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(4, updated.getStates().size());
        }
        {
            val updated = ds.createOrUpdateTransition(original.getId(),
                                                      "S_I1",
                                                      "START",
                                                      "INTERMEDIATE_1",
                                                      TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                                      new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                                      List.of("NO_ACTION"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(1, updated.getTicketStateTransitions().size());
            assertEquals(1, updated.getTicketStateTransitions().get("START").size());
        }
        {
            val updated = ds.createOrUpdateTransition(original.getId(),
                                                      "S_I2",
                                                      "START",
                                                      "INTERMEDIATE_2",
                                                      TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                                      new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                                      List.of("NO_ACTION"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(1, updated.getTicketStateTransitions().size());
            assertEquals(2, updated.getTicketStateTransitions().get("START").size());
        }
        {
            val updated = ds.createOrUpdateTransition(original.getId(),
                                                      "I1_E",
                                                      "INTERMEDIATE_1",
                                                      "END",
                                                      TicketStateTransition.TicketStateTransitionType.DEFAULT,
                                                      new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                                      List.of("NO_ACTION"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(2, updated.getTicketStateTransitions().size());
            assertEquals(1, updated.getTicketStateTransitions().get("INTERMEDIATE_1").size());
        }
        {
            val updated = ds.createOrUpdateTransition(original.getId(),
                                                      "I2_E",
                                                      "INTERMEDIATE_2",
                                                      "END",
                                                      TicketStateTransition.TicketStateTransitionType.DEFAULT,
                                                      new Rule(Rule.RuleType.JSON_RULE, "{}"),
                                                      List.of("NO_ACTION"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(3, updated.getTicketStateTransitions().size());
            assertEquals(1, updated.getTicketStateTransitions().get("INTERMEDIATE_2").size());
        }
        {
            val updated = ds.createOrUpdateSelectionRule(original.getId(),
                                                         "R1",
                                                         new Rule(Rule.RuleType.JSON_RULE, "{}"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(1, updated.getSelectionRules().size());
            assertNotNull(updated.getSelectionRules().get("R1"));
        }
        {
            val updated = ds.createOrUpdateSelectionRule(original.getId(),
                                                         "R2",
                                                         new Rule(Rule.RuleType.JSON_RULE, "{}"))
                    .orElse(null);
            assertNotNull(updated);
            assertEquals(2, updated.getSelectionRules().size());
            assertNotNull(updated.getSelectionRules().get("R2"));
        }
    }

}