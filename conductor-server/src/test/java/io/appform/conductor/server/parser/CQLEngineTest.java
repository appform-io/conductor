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

package io.appform.conductor.server.parser;

import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.schema.fields.StringFieldSchema;
import io.appform.conductor.model.workflow.*;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
class CQLEngineTest {

    @Test
    @Disabled
    void test() {
        val schema = new Schema("TS1",
                                "Test Schema",
                                "Test",
                                1,
                                SchemaState.ACTIVE,
                                "U1",
                                "U1",
                                List.of(
                                        new StringFieldSchema("TS1-firstName",
                                                              "firstName",
                                                              "First Name",
                                                              "",
                                                              null,
                                                              null,
                                                              null,
                                                              false,
                                                              new Date(),
                                                              new Date(),
                                                              200,
                                                              null,
                                                              null),
                                        new StringFieldSchema("TS1-lastName",
                                                              "lastName",
                                                              "Last Name",
                                                              "",
                                                              null,
                                                              null,
                                                              null,
                                                              false,
                                                              new Date(),
                                                              new Date(),
                                                              200,
                                                              null,
                                                              null)
                                       ),
                                new Date(),
                                new Date());
        val workflow = createWorkflow(schema);
        val workflowStore = mock(WorkflowStore.class);
        val schemaStore = mock(SchemaStore.class);
        when(schemaStore.get(anyString())).thenReturn(Optional.of(schema));

        val parser = new CQLEngine(workflowStore, schemaStore);
        parser.parse("select workflowId, title, age(), ticket.subject.name from tickets.TWF" +
                             " where assignedToGroupId in ('G1', 'G2') and isTerminal(state)" +
                             " and fields.issueState = 'KA'");
    }

    private static Workflow createWorkflow(Schema schema) {
        val states = Map.of(
                "START",
                new TicketState("START",
                                "Start",
                                "",
                                false,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                new Date(),
                                new Date()),
                "FIRST_NAME_COLLECTED",
                new TicketState("FIRST_NAME_COLLECTED",
                                "First name collected",
                                "",
                                false,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                new Date(),
                                new Date()),
                "FULL_NAME_COLLECTED",
                new TicketState("FULL_NAME_COLLECTED",
                                "Full name collected",
                                "",
                                true,
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                new Date(),
                                new Date())
                           );
        val transitions = Map.of(
                "START", List.of(new TicketStateTransition("S_F_N_C",
                                                           "START",
                                                           "FIRST_NAME_COLLECTED",
                                                           TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                                           new Rule(Rule.RuleType.HOPE,
                                                                    "pointer.exists('/ticket/fields/firstName') == " +
                                                                            "true"),
                                                           List.of(),
                                                           "WF1",
                                                           new Date(),
                                                           new Date())),
                "FIRST_NAME_COLLECTED", List.of(new TicketStateTransition("F_N_F_N_C",
                                                                          "FIRST_NAME_COLLECTED",
                                                                          "FULL_NAME_COLLECTED",
                                                                          TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                                                          new Rule(Rule.RuleType.HOPE,
                                                                                   "pointer.exists" +
                                                                                           "('/ticket/fields/lastName" +
                                                                                           "') == " +
                                                                                           "true"),
                                                                          List.of(),
                                                                          "WF1",
                                                                          new Date(),
                                                                          new Date()))
                                );
        val workflow = new Workflow("TWF",
                                    "Test workflow",
                                    "",
                                    schema.getId(),
                                    new Template(Template.Type.STRING_SUBSTITUTION, "Ticket for ${firstName}"),
                                    new Template(Template.Type.STRING_SUBSTITUTION,
                                                 "This is a ticket for ${firstName}"),
                                    new Template(Template.Type.FIXED,
                                                 "{ \"type\" : \"PHONE\", \"subType\" : \"\", \"value\" : " +
                                                         "\"1234567890\", \"verificationStatus\" : " +
                                                         "\"SYSTEM_VERIFIED\" }"),
                                    states,
                                    transitions,
                                    List.of(),
                                    "START",
                                    Map.of("R1", new Rule(Rule.RuleType.HOPE, "pointer.exists('/firstName') == true"),
                                           "R2", new Rule(Rule.RuleType.HOPE, "pointer.exists('/lastName') == true")),
                                    WorkflowState.ACTIVE,
                                    new Date(),
                                    new Date());
        return workflow;
    }


}