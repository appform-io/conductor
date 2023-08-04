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

package io.appform.conductor.server.ticketmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.schema.fields.StringFieldSchema;
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.model.workflow.*;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.ruleengines.HopeRuleEvaluator;
import io.appform.conductor.server.ruleengines.JsonRuleEvaluator;
import io.appform.conductor.server.ruleengines.RuleEngine;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.subjectmanagement.impl.DBSubjectStore;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredAddress;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectID;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectSummary;
import io.appform.conductor.server.templateengines.FixedObjectTemplateEvaluator;
import io.appform.conductor.server.templateengines.FixedTextTemplateEvaluator;
import io.appform.conductor.server.templateengines.StringSubstitutionTextTemplateEvaluator;
import io.appform.conductor.server.templateengines.TemplateEngine;
import io.appform.conductor.server.ticketmanagement.impl.DBTicketStore;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.workflowmanagement.WorkflowSelector;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static io.appform.conductor.server.utils.ConductorServerUtils.ticketToJsonNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
@ExtendWith(DBTestExtension.class)
class TicketManagerTest {

    @Test
    @SneakyThrows
    void testProcessRaw(BalancedDBShardingBundle<TestConfig> bundle) {
        val creator = new UserSummary("U001",
                                      UserType.HUMAN,
                                      "",
                                      "",
                                      UserState.ACTIVE,
                                      new Date(),
                                      new Date());

        val schema = new Schema("TS1",
                                "Test Schema",
                                "Test",
                                1,
                                SchemaState.ACTIVE,
                                creator.getId(),
                                creator.getId(),
                                List.of(
                                        new StringFieldSchema("TS1-firstName",
                                                              "firstName",
                                                              "First Name",
                                                              "",
                                                              true,
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
                                                              true,
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
        val states = Map.of(
                "START",
                new TicketState("START", "Start", "", false, List.of(), List.of(), List.of(), new Date(), new Date()),
                "FIRST_NAME_COLLECTED",
                new TicketState("FIRST_NAME_COLLECTED", "First name collected", "", false, List.of(), List.of(), List.of(), new Date(), new Date()),
                "FULL_NAME_COLLECTED",
                new TicketState("FULL_NAME_COLLECTED", "Full name collected", "", true, List.of(), List.of(), List.of(), new Date(), new Date())
                           );
        val transitions = Map.of(
                "START", List.of(new TicketStateTransition("S_F_N_C",
                                                           "START",
                                                           "FIRST_NAME_COLLECTED",
                                                           TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                                           new Rule(Rule.RuleType.HOPE,
                                                                    "pointer.exists('/ticket/fields/firstName') == true"),
                                                           List.of(),
                                                           new Date(),
                                                           new Date())),
                "FIRST_NAME_COLLECTED", List.of(new TicketStateTransition("F_N_F_N_C",
                                                                          "FIRST_NAME_COLLECTED",
                                                                          "FULL_NAME_COLLECTED",
                                                                          TicketStateTransition.TicketStateTransitionType.EVALUATED,
                                                                          new Rule(Rule.RuleType.HOPE,
                                                                                   "pointer.exists('/ticket/fields/lastName') == " +
                                                                                           "true"),
                                                                          List.of(),
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
                                    "START",
                                    Map.of("R1", new Rule(Rule.RuleType.HOPE, "pointer.exists('/firstName') == true"),
                                           "R2", new Rule(Rule.RuleType.HOPE, "pointer.exists('/lastName') == true")),
                                    WorkflowState.ACTIVE,
                                    new Date(),
                                    new Date());
        val mapper = new ObjectMapper();
        val ts = new DBTicketStore(bundle.createParentObjectDao(StoredTicketSkeleton.class),
                                   bundle.createRelatedObjectDao(StoredFieldValue.class),
                                   bundle.createRelatedObjectDao(StoredComment.class),
                                   bundle.createRelatedObjectDao(StoredAttachment.class),
                                   mapper);
        val sStore = mock(SchemaStore.class);
        when(sStore.get(anyString())).thenReturn(Optional.of(schema));
        val uStore = mock(UserStore.class);
        when(uStore.getById(anyString())).thenReturn(Optional.of(creator));
        val gStore = mock(GroupStore.class);
        when(gStore.get(anyString())).thenReturn(Optional.empty());
        val aStore = mock(ActionStore.class);
        when(aStore.read(anyString())).thenReturn(Optional.empty());
        val wStore = mock(WorkflowStore.class);
        when(wStore.list(anySet())).thenReturn(List.of(workflow));
        val suStore = new DBSubjectStore(bundle.createParentObjectDao(StoredSubjectSummary.class),
                                         bundle.createRelatedObjectDao(StoredSubjectID.class),
                                         bundle.createRelatedObjectDao(StoredAddress.class));

        val re = new RuleEngine(new HopeRuleEvaluator(), new JsonRuleEvaluator(mapper));
        val te = new TemplateEngine(new FixedTextTemplateEvaluator(),
                                    new StringSubstitutionTextTemplateEvaluator(mapper),
                                    new FixedObjectTemplateEvaluator(mapper));
        val workflowSelector = new WorkflowSelector(wStore, re);
        workflowSelector.start();
        val actionExecutor = mock(ActionExecutor.class);
        when(actionExecutor.execute(any(Action.class), any(ActionExecutor.ActionEvalData.class)))
                .thenReturn(ActionExecutionResult.SUCCESS);
        val ticketManager = new TicketManager(ts,
                                              sStore,
                                              uStore,
                                              gStore,
                                              suStore,
                                              aStore,
                                              wStore,
                                              workflowSelector,
                                              new TicketFieldMapper(),
                                              re,
                                              te,
                                              actionExecutor,
                                              mapper);
        val res = ticketManager.processRaw(mapper.readTree("""
                                                                   {
                                                                     "firstName" : "Santanu"
                                                                   }
                                                                   """));
        assertTrue(res.isPresent());
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ticketToJsonNode(mapper, res.get(), schema)));
        assertEquals("FIRST_NAME_COLLECTED", res.get().getSummary().getTicketState().getId());
        val res2 = ticketManager.processRaw(mapper.readTree("""
                                                                    {
                                                                      "lastName" : "Sinha"
                                                                   }
                                                                    """));
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ticketToJsonNode(mapper, res2.get(), schema)));
        assertEquals("FULL_NAME_COLLECTED", res2.get().getSummary().getTicketState().getId());
    }

}