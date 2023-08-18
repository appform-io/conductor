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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.subject.Gender;
import io.appform.conductor.model.subject.SubjectID;
import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.subject.SubjectSummary;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.TicketSummary;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.TicketFilterType;
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
import io.appform.conductor.server.ticketmanagement.statemachine.TicketEvent;
import io.appform.conductor.server.ticketmanagement.statemachine.TicketStateMachine;
import io.appform.conductor.server.ticketmanagement.statemachine.TransitionHandler;
import io.appform.conductor.server.ticketmanagement.statemachine.TriggerStrategy;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.workflowmanagement.WorkflowSelector;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
    private final WorkflowStore workflowStore;
    private final WorkflowSelector workflowSelector;
    private final TicketFieldMapper fieldMapper;
    private final RuleEngine ruleEngine;
    private final TemplateEngine templateEngine;
    private final ActionExecutor actionExecutor;
    private final ObjectMapper mapper;

    @SneakyThrows
    public Optional<TicketDetails> createTicket(
            final String title,
            final String description,
            final SubjectIDType subjectIDType,
            final String subjectIdSubType,
            final String subjectIdValue,
            final String workflowId) {
        val workflow = workflowStore.read(workflowId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW)
                        .build());
        val schema = fetchSchema(workflow.getId(), workflow.getSchemaId());
        val sId = SubjectID.builder()
                .type(subjectIDType)
                .subType(subjectIdSubType)
                .value(subjectIdValue)
                .build();
        val subject = Objects.requireNonNull(fetchSubject(sId).orElseGet(() -> createEmptySubject(sId)));
        val startState = workflow.getStartStateId();
        val ticket = Objects.requireNonNull(
                ticketStore.create(UUID.randomUUID().toString(),
                                   title,
                                   description,
                                   workflowId,
                                   subject.getGlobalId(),
                                   startState,
                                   TicketPriority.MEDIUM,
                                   List.of())
                        .map(skeleton -> ticketDetails(skeleton, workflow, subject))
                        .orElse(null));
        return runTransitions(mapper.createObjectNode(),
                              workflow,
                              subject,
                              ticket,
                              schema,
                              TicketEvent.EventSource.TICKET_UPDATE);
    }

    public Optional<TicketDetails> readTicket(final String ticketId) {
        val ticket = ticketStore.read(ticketId, true).orElse(null);
        if (null == ticket) {
            return Optional.empty();
        }
        val subject = subjectStore.getSubject(ticket.getSubjectId()).orElse(null);
        if (null == subject) {
            return Optional.empty();
        }
        val workflow = workflowStore.read(ticket.getWorkflowId()).orElse(null);
        if (null == workflow) {
            return Optional.empty();
        }
        return Optional.of(ticketDetails(ticket, workflow, subject.getSummary()));
    }

    public TicketGistListResult ticketsForSubject(final String subjectId, String start, int size) {

        return search(List.of(TicketSubjectEquals.builder()
                                      .subjectId(subjectId)
                                      .build()),
                      List.of(),
                      start,
                      size);
    }

    @SneakyThrows
    public TicketGistListResult search(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size) {
        val result = ticketStore.list(
                ticketFilters,
                fieldFilters, start, size, Map.of());
        return TicketGistListResult.builder()
                .next(result.getNext())
                .results(result.getResults()
                                 .stream()
                                 .map(skel -> {
                                     val wf = workflowStore.read(skel.getWorkflowId());
                                     val currState = wf.map(workflow -> workflow.getStates()
                                             .get(skel.getTicketStateId()));
                                     return new TicketGist(skel.getTicketId(),
                                                           skel.getTitle(),
                                                           wf.map(Workflow::getDisplayName).orElse(""),
                                                           currState.map(TicketState::getDisplayName).orElse(""),
                                                           currState.map(TicketState::isTerminal).orElse(false),
                                                           skel.getPriority(),
                                                           skel.getCreated(),
                                                           skel.getUpdated());
                                 })
                                 .toList())
                .build();
    }

    @SneakyThrows
    public TicketSkeletonListResult list(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size) {
        val fields = fieldFilters.isEmpty()
                     ? Map.<String, FieldSchema>of()
                     : ticketFilters.stream()
                             .filter(tf -> tf.getType().equals(TicketFilterType.WORKFLOW_EQUALS))
                             .map(TicketWorkflowEquals.class::cast)
                             .map(TicketWorkflowEquals::getWorkflowId)
                             .findFirst()
                             .flatMap(workflowStore::read)
                             .map(Workflow::getSchemaId)
                             .flatMap(schemaStore::get)
                             .map(schema -> schema.getFields().stream())
                             .map(fieldSchemaStream -> fieldSchemaStream.collect(Collectors.toMap(FieldSchema::getId,
                                                                                                  Function.identity())))
                             .orElse(Map.of());
        if (fields.isEmpty()) {
            return TicketSkeletonListResult.EMPTY;
        }
        return ticketStore.list(ticketFilters, fieldFilters, start, size, fields);
    }

    @SneakyThrows
    public Optional<TicketDetails> processFormFieldUpdate(
            final String ticketId,
            final MultivaluedMap<String, String> form) {
        val ticket = ticketStore.read(ticketId, true).orElse(null);
        if (null == ticket) {
            return Optional.empty();
        }

        val workflow = workflowStore.read(ticket.getWorkflowId()).orElse(null);
        if (null == workflow) {
            return Optional.empty();
        }
        val subject = subjectStore.getSubject(ticket.getSubjectId()).orElse(null);
        if (null == subject) {
            return Optional.empty();
        }
        if (workflow.getStates().get(ticket.getTicketStateId()).isTerminal()) {
            return Optional.of(ticketDetails(ticket, workflow, subject.getSummary()));
        }
        val schema = schemaStore.get(workflow.getSchemaId()).orElse(null);
        if (null == schema) {
            return Optional.empty();
        }
        val node = mapper.createObjectNode();
        form.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("ticketField-"))
                .forEach(e -> {
                    val key = e.getKey().replaceAll("^ticketField-", "");
                    val valueList = e.getValue();
                    val value = valueList.size() == 1 ? valueList.get(0) : valueList;
                    node.set(key, mapper.valueToTree(value));
                });
        return processTicketUpdate(node, schema, workflow, subject.getSummary(),
                                   (payload, fields) -> {
                                       val fs = schema.getFields()
                                               .stream().collect(Collectors.toMap(FieldSchema::getId,
                                                                                  Function.identity()));
                                       val objectNode = (ObjectNode) payload;
                                       objectNode.removeAll();
                                       fields.forEach(field -> {
                                           val fieldSchema = fs.get(field.getSchemaFieldId());
                                           objectNode.set(fieldSchema.getName(),
                                                          ConductorServerUtils.mapValueToJsonNode(mapper,
                                                                                                  fieldSchema,
                                                                                                  field.getValue()));
                                       });
                                   },
                                   TicketEvent.EventSource.TICKET_UPDATE);
    }

    @SneakyThrows
    public Optional<TicketDetails> processFormSummaryUpdate(
            final String ticketId,
            final String title,
            final String description,
            final TicketPriority priority) {
        val ticket = ticketStore.update(ticketId,
                                        ticketSkeleton -> ticketSkeleton.setTitle(title)
                                                .setDescription(description)
                                                .setPriority(priority),
                                        List.of()).orElse(null);
        if (null == ticket) {
            return Optional.empty();
        }

        val workflow = workflowStore.read(ticket.getWorkflowId()).orElse(null);
        if (null == workflow) {
            return Optional.empty();
        }
        val subject = subjectStore.getSubject(ticket.getSubjectId()).orElse(null);
        if (null == subject) {
            return Optional.empty();
        }
        if (workflow.getStates().get(ticket.getTicketStateId()).isTerminal()) {
            return Optional.of(ticketDetails(ticket, workflow, subject.getSummary()));
        }
        val schema = schemaStore.get(workflow.getSchemaId()).orElse(null);
        if (null == schema) {
            return Optional.empty();
        }
        val node = mapper.createObjectNode();
/*        node.put("title", title);
        node.put("description", description);
        node.put("priority", priority.name());*/
        return processTicketUpdate(node, schema, workflow, subject.getSummary(), (payload, fields) -> {}, TicketEvent.EventSource.TICKET_UPDATE);
    }

    @SneakyThrows
    public Optional<TicketDetails> processRaw(final JsonNode payload) {
        val workflow = selectWorkflow(payload);
        val sId = extractSubjectId(payload, workflow);

        val subject = Objects.requireNonNull(fetchSubject(sId).orElseGet(() -> createEmptySubject(sId)));
        val schema = fetchSchema(workflow.getId(), workflow.getSchemaId());
        return processTicketUpdate(payload, schema, workflow, subject, (node, fields) -> {}, TicketEvent.EventSource.INGRESS_RAW);
    }

    @SneakyThrows
    public Optional<TicketDetails> processCallback(final String ticketId, final JsonNode payload) {
        //TODO: Remove duplicate code of context building from all the different sources
        val ticket = ticketStore.read(ticketId, true)
                .orElse(null);
        if (null == ticket) {
            return Optional.empty();
        }
        val workflow = workflowStore.read(ticket.getWorkflowId()).orElse(null);
        if (null == workflow) {
            return Optional.empty();
        }
        val subject = subjectStore.getSubject(ticket.getSubjectId()).orElse(null);
        if (null == subject) {
            return Optional.empty();
        }
        if (workflow.getStates().get(ticket.getTicketStateId()).isTerminal()) {
            return Optional.of(ticketDetails(ticket, workflow, subject.getSummary()));
        }
        val schema = schemaStore.get(workflow.getSchemaId()).orElse(null);
        if (null == schema) {
            return Optional.empty();
        }
        return processTicketUpdate(payload, schema, workflow, subject.getSummary(), (node, fields) -> {}, TicketEvent.EventSource.INGRESS_CALLBACK);
    }

    public boolean assignTicketToGroup(final String ticketId, final String groupId) {
        return groupStore.read(groupId)
                .flatMap(group -> ticketStore.update(ticketId,
                                                     ticketSkeleton -> ticketSkeleton.setAssignedToGroupId(groupId),
                                                     List.of()))
                .filter(ticketSkeleton -> groupId.equals(ticketSkeleton.getAssignedToGroupId()))
                .isPresent();
    }
    private Optional<TicketDetails> processTicketUpdate(
            JsonNode payload,
            Schema schema,
            Workflow workflow,
            SubjectSummary subject,
            BiConsumer<JsonNode, List<TicketFieldData>> mappedFieldDecorator,
            TicketEvent.EventSource source) {
        val fieldMappingResult = fieldMapper.map(schema, payload);
        ConductorServerUtils.ensureCondition(fieldMappingResult.getErrors().isEmpty(),
                                             ConductorErrorCode.TICKET_SCHEMA_VALIDATION_FAILURE,
                                             Map.of("errors", fieldMappingResult.getErrors()));
        mappedFieldDecorator.accept(payload, fieldMappingResult.getData());
        val terminalStates = findTerminalStates(workflow);
        val ticket = Objects.requireNonNull(
                updateExistingTicket(workflow, subject, terminalStates, fieldMappingResult.getData())
                        .orElseGet(() -> createNewTicket(payload, workflow, fieldMappingResult, subject)));
        if (ticket.getSummary().getTicketState().isTerminal()) {
            log.info("Ticket {} is already in terminal state. Payload ignored. Payload: {}",
                     ticket.getSummary().getId(), payload);
            return Optional.of(ticket);
        }
        return runTransitions(payload,
                              workflow,
                              subject,
                              ticket,
                              schema,
                              source);
    }

    private Optional<TicketDetails> runTransitions(
            JsonNode payload,
            Workflow workflow,
            SubjectSummary subject,
            @NonNull TicketDetails ticket,
            Schema schema,
            TicketEvent.EventSource source) {
        TicketStateMachine stateMachine = new TicketStateMachine(
                workflow.getTicketStateTransitions(),
                schema,
                ticket,
                mapper,
                ruleEngine,
                new TransitionHandler() {

                    @Override
                    public TicketDetails beforeTransition(TicketStateTransition transition, TicketDetails ticket, TicketEvent event) {
                        log.info("Ticket {} beforeTransition is now in state: {}",
                                 ticket.getSummary().getId(),
                                 ticket.getSummary().getTicketState().getDisplayName());
                        return ticket;
                    }

                    @Override
                    public TicketDetails onTransition(TicketStateTransition transition, TicketDetails ticket, TicketEvent event) {
                        val updatedTicket =  Objects.requireNonNull(updateTicketState(workflow, ticket, transition)
                                .map(t -> ticketDetails(t, workflow, subject))
                                .orElse(null));
                        log.info("Ticket {} onTransitions is now in state: {}",
                                ticket.getSummary().getId(),
                                ticket.getSummary().getTicketState().getDisplayName());
                        return updatedTicket;
                    }

                    @Override
                    public TicketDetails afterTransition(TicketStateTransition transition, TicketDetails ticket, TicketEvent event) {
                        val ticketId = ticket.getSummary().getId();
                        var updatedTicket = ticket;
                        for (val actionId : transition.getActionIds()) {
                            val actionExecutionData = new ActionExecutor.ActionEvalData(
                                    workflow,
                                    schema,
                                    ticket,
                                    ConductorServerUtils.ticketToJsonNode(mapper, ticket, schema),
                                    event.getPayload());
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
                            updatedTicket =  readDetails(workflow, subject, ticketId);
                        }
                        return updatedTicket;
                    }
                });
        stateMachine.trigger(new TicketEvent(source, payload), TriggerStrategy.EXECUTE_ALL);
        return Optional.of(stateMachine.getTicket());
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
        return ticketStore.updateState(ticket.getSummary().getId(),
                                       workflow.getStates().get(finalMatchingTransition.getTo()));
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
                                        UUID.randomUUID().toString(),
                                        "",
                                        null,
                                        Gender.OTHER)
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
                                                   userStore.getById(skeleton.getCreatedByUserId()).orElse(null),
                                                   Objects.nonNull(skeleton.getAssignedToGroupId())
                                                   ? groupStore.read(skeleton.getAssignedToGroupId()).orElse(null)
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
                                 List.of()); //TODO::ACTION
    }

}
