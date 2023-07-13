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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.subject.SubjectID;
import io.appform.conductor.model.subject.SubjectSummary;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.TicketSummary;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketStateIn;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketSubjectEquals;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketWorkflowEquals;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.ruleengines.RuleEngine;
import io.appform.conductor.server.schemamanagement.SchemaOpValidationResult;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.templateengines.TemplateEngine;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.workflowmanagement.WorkflowSelector;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class TicketManager {

    private final TicketStore ticketStore;
    private final SchemaStore schemaStore;
    private final UserStore userStore;
    private final GroupStore groupStore;
    private final SubjectStore subjectStore;
    private final ActionStore actionStore;
    private final WorkflowSelector workflowSelector;
    private final TicketFieldMapper fieldMapper;
    private final RuleEngine ruleEngine;
    private final TemplateEngine templateEngine;
    private final ActionExecutor actionExecutor;
    private final ObjectMapper mapper;

    @SneakyThrows
    public Optional<TicketDetails> processRaw(final JsonNode payload) {
        val workflow = selectWorkflow(payload);
        val schema = fetchSchema(workflow.getId(), workflow.getSchemaId());
        val fieldMappingResult = fieldMapper.map(schema, payload);
        ConductorServerUtils.ensureCondition(fieldMappingResult.getErrors().isEmpty(),
                                             ConductorErrorCode.TICKET_SCHEMA_VALIDATION_FAILURE,
                                             Map.of("errors", fieldMappingResult.getErrors()));
        val sId = extractSubjectId(payload, workflow);

        val subject = Objects.requireNonNull(fetchSubject(sId).orElseGet(() -> createEmptySubject(sId)));
        val terminalStates = findTerminalStates(workflow);
        val ticket = Objects.requireNonNull(
                updateExistingTicket(workflow, subject, terminalStates, fieldMappingResult.getData())
                        .orElseGet(() -> createNewTicket(payload, workflow, fieldMappingResult, subject)));
        if(ticket.getSummary().getTicketState().isTerminal()) {
            log.info("Ticket {} is already in terminal state. Payload ignored. Payload: {}",
                     ticket.getSummary().getId(), payload);
            return Optional.of(ticket);
        }
        return runTransitions(payload,
                              workflow,
                              subject,
                              ticket,
                              schema);
    }

    private Optional<TicketDetails> runTransitions(
            JsonNode payload,
            Workflow workflow,
            SubjectSummary subject,
            @NonNull TicketDetails ticket,
            Schema schema) {
        var evalTicket = ticket;
        val evalDataJson = mapper.createObjectNode();
        evalDataJson.set("payload", payload);
        do {

            evalDataJson.set("ticket", ConductorServerUtils.ticketToJsonNode(mapper, evalTicket, schema));
            val ticketSummary = evalTicket.getSummary();
            val currState = ticketSummary.getTicketState();
            val ticketId = ticketSummary.getId();
            if (currState.isTerminal()) {
                log.info("Ticket {} is already in terminal state {}",
                         ticketId,
                         currState.getDisplayName());
                break;
            }
            val transitions = workflow.getTicketStateTransitions().get(currState.getId());
            var matchingTransition = transitions.stream()
                    .filter(ticketStateTransition -> switch (ticketStateTransition.getType()) {
                        case EVALUATED -> ruleEngine.evaluate(ticketStateTransition.getRule(), evalDataJson);
                        case DEFAULT -> false;
                    })
                    .findFirst()
                    .orElse(null);
            if (null == matchingTransition) {
                matchingTransition = transitions.stream()
                        .filter(transition -> transition.getType()
                                .equals(TicketStateTransition.TicketStateTransitionType.DEFAULT))
                        .findAny()
                        .orElse(null);
            }
            else {
                log.debug("Found matching transition: {}", matchingTransition);
            }
            if (null == matchingTransition) {
                log.info("No possible transitions found for ticket: {}. Will stop state machine execution.", ticketId);
                break;
            }
            val finalMatchingTransition = matchingTransition;
            evalTicket = Objects.requireNonNull(updateTicketState(workflow, evalTicket, finalMatchingTransition)
                                                        .map(t -> ticketDetails(t, workflow, subject))
                                                        .orElse(null));
            log.info("Ticket {} is now in state: {}",
                     ticketId,
                     evalTicket.getSummary().getTicketState().getDisplayName());
            for (val actionId : matchingTransition.getActionIds()) {
                val actionExecutionData = new ActionExecutor.ActionEvalData(
                        workflow,
                        schema,
                        evalTicket,
                        ConductorServerUtils.ticketToJsonNode(mapper, evalTicket, schema),
                        payload);
                val action = actionStore.read(actionId)
                        .orElseThrow(() -> ConductorException.builder()
                                .errorCode(ConductorErrorCode.TICKET_MGMT_NO_ACTION)
                                .context(Map.of("ticketId", ticketId,
                                                "workflowId", workflow.getId(),
                                                "actionId", actionId))
                                .build());
                val result = actionExecutor.execute(action, actionExecutionData);
                log.info("Status for action execution: {}", result);
                //Always read the updated evalTicket data before applying new transitions
                evalTicket = readDetails(workflow, subject, ticketId);
            }
        } while (true);
        return Optional.of(evalTicket);
    }

    private TicketDetails readDetails(Workflow workflow, SubjectSummary subject, String ticketId) {
        return ticketStore.read(ticketId, true)
                .map(t -> ticketDetails(t, workflow, subject))
                .orElse(null);
    }

    private Optional<TicketSkeleton> updateTicketState(
            Workflow workflow,
            TicketDetails ticket,
            TicketStateTransition finalMatchingTransition) {
        return ticketStore.update(ticket.getSummary().getId(),
                                  t -> t.setTicketStateId(workflow.getStates()
                                                                  .get(finalMatchingTransition.getTo())
                                                                  .getId()),
                                  List.of());
    }

    private SubjectID extractSubjectId(JsonNode payload, Workflow workflow) {
        return templateEngine.evaluateToObject(workflow.getSubjectIdTemplate(),
                                               payload,
                                               SubjectID.class)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_SUBJECT_ID_EXTRACTION_FAILURE)
                        .build());
    }

    private Workflow selectWorkflow(JsonNode payload) {
        return workflowSelector.findWorkflow(payload)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW)
                        .build());
    }

    private Schema fetchSchema(String workflowId, String schemaId) {
        return schemaStore.get(schemaId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_SCHEMA)
                        .context(Map.of("workflowId", workflowId,
                                        "schemaId", schemaId))
                        .build());
    }

    private SubjectSummary createEmptySubject(SubjectID sId) {
        return subjectStore.saveSubject(List.of(sId),
                                        UUID.randomUUID()
                                                .toString(),
                                        "",
                                        null)
                .orElse(null);
    }

    private Optional<SubjectSummary> fetchSubject(SubjectID sId) {
        return subjectStore.lookupSummaryById(sId)
                .stream()
                .findFirst();
    }

    private static Set<String> findTerminalStates(Workflow workflow) {
        return workflow.getStates()
                .values()
                .stream()
                .filter(TicketState::isTerminal)
                .map(TicketState::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private TicketDetails createNewTicket(
            JsonNode payload,
            Workflow workflow,
            SchemaOpValidationResult<List<TicketFieldData>> fieldMappingResult,
            SubjectSummary subject) {
        return ticketStore.create(UUID.randomUUID().toString(),
                                  templateEngine.evaluateToText(
                                                  workflow.getTitleTemplate(),
                                                  payload)
                                          .orElse("Default"),
                                  templateEngine.evaluateToText(
                                          workflow.getDescriptionTemplate(),
                                          payload).orElse(""),
                                  workflow.getId(),
                                  subject.getGlobalId(),
                                  workflow.getStartStateId(),
                                  TicketPriority.MEDIUM,
                                  Objects.requireNonNullElse(
                                          fieldMappingResult.getData(),
                                          List.of()))
                .map(skeleton -> ticketDetails(skeleton, workflow, subject))
                .orElse(null);
    }

    private Optional<TicketDetails> updateExistingTicket(
            Workflow workflow,
            SubjectSummary subject,
            Set<String> terminalStates,
            List<TicketFieldData> fields) {
        val existing = ticketStore.list(List.of(new TicketWorkflowEquals(workflow.getId()),
                                                new TicketSubjectEquals(subject.getGlobalId()),
                                                new TicketStateIn(terminalStates, true)),
                                        List.of(),
                                        null,
                                        1,
                                        Map.of())
                .getResults()
                .stream()
                .findAny();
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        val summary = existing.get();
        return ticketStore.update(summary.getTicketId(), t -> t, fields)
                .map(skeleton -> ticketDetails(skeleton, workflow, subject));
    }

    private TicketDetails ticketDetails(
            final TicketSkeleton skeleton,
            final Workflow workflow,
            SubjectSummary subject) {
        return new TicketDetails(new TicketSummary(skeleton.getTicketId(),
                                                   skeleton.getTitle(),
                                                   skeleton.getDescription(),
                                                   skeleton.getWorkflowId(),
                                                   userStore.getById(skeleton.getTicketId()).orElse(null),
                                                   Objects.nonNull(skeleton.getAssignedToGroupId())
                                                   ? groupStore.get(skeleton.getAssignedToGroupId()).orElse(null)
                                                   : null,
                                                   Objects.nonNull(skeleton.getAssignedToUserId())
                                                   ? userStore.getById(skeleton.getAssignedToUserId()).orElse(null)
                                                   : null,
                                                   subject,
                                                   workflow.getStates().get(skeleton.getTicketStateId()),
                                                   skeleton.getPriority(),
                                                   skeleton.getCreated(),
                                                   skeleton.getUpdated()),
                                 skeleton.getFields(),
                                 List.of(), //TODO::COMMENTS
                                 List.of()); //TODO::ACTION
    }

}
