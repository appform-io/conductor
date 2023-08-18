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
import io.appform.conductor.server.ticketmanagement.statemachine.Trigger;
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

import static io.appform.conductor.server.utils.ConductorServerUtils.ensureNonNull;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class TicketManager {

    public static final String TICKET_ID = "ticketId";
    public static final String WORKFLOW_ID = "workflowId";
    public static final String SCHEMA_ID = "schemaId";
    public static final String SUBJECT_ID = "subjectId";
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
                              Trigger.TriggerSource.TICKET_UPDATE);
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
                                   Trigger.TriggerSource.TICKET_UPDATE);
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
        return processTicketUpdate(node, schema, workflow, subject.getSummary(), (payload, fields) -> {}, Trigger.TriggerSource.TICKET_UPDATE);
    }

    @SneakyThrows
    public Optional<TicketDetails> processRaw(final JsonNode payload) {
        trigger(Trigger.TriggerSource.INGRESS_RAW, TicketStrategy.CREATE_FROM_RAW_DATA, payload,(node, fields) -> {});
        val workflow = selectWorkflow(payload);
        val sId = extractSubjectId(payload, workflow);

        val subject = Objects.requireNonNull(fetchSubject(sId).orElseGet(() -> createEmptySubject(sId)));
        val schema = fetchSchema(workflow.getId(), workflow.getSchemaId());
        return processTicketUpdate(payload, schema, workflow, subject, (node, fields) -> {}, Trigger.TriggerSource.INGRESS_RAW);
    }


    public Optional<TicketDetails> trigger(Trigger.TriggerSource source,
                                           TicketStrategy ticketStrategy,
                                           JsonNode payload,
                                           BiConsumer<JsonNode, List<TicketFieldData>> mappedFieldDecorator) {
        TicketStateMachineContext ticketStateMachineContext = new TicketStateMachineContext();
        addTicketSkeleton(ticketStrategy, payload, ticketStateMachineContext);
        addWorkflow(ticketStrategy, payload, ticketStateMachineContext);
        addSchema(ticketStrategy, ticketStateMachineContext);
        addSubject(ticketStrategy, payload, ticketStateMachineContext);
        idempotencyCheckIfAny(ticketStrategy, ticketStateMachineContext);

        //DataDecorator
        val fieldMappingResult = fieldMapper.map(ticketStateMachineContext.getSchema(), payload);
        ConductorServerUtils.ensureCondition(fieldMappingResult.getErrors().isEmpty(),
                ConductorErrorCode.TICKET_SCHEMA_VALIDATION_FAILURE,
                Map.of("errors", fieldMappingResult.getErrors()));
        mappedFieldDecorator.accept(payload, fieldMappingResult.getData());


        if(ticketStateMachineContext.getTicketSkeleton() == null) {
            createNewTicket(payload, ticketStateMachineContext, fieldMappingResult);
        }
        if (ticketStateMachineContext.getWorkflow().getStates().get(ticketStateMachineContext.getTicketSkeleton().getTicketStateId()).isTerminal()) {
            switch (ticketStrategy.alreadyEndedStrategy) {
                case ABORT :
                    return Optional.of(ticketDetails(ticketStateMachineContext.getTicketSkeleton(),
                            ticketStateMachineContext.getWorkflow(),
                            ticketStateMachineContext.getSubject()));
                case CREATE_NEW :
                    createNewTicket(payload, ticketStateMachineContext, fieldMappingResult);
                    break;
            }
        }
        ticketStateMachineContext.setTicketDetails(ticketDetails(ticketStateMachineContext.getTicketSkeleton(),
                ticketStateMachineContext.getWorkflow(),
                ticketStateMachineContext.getSubject()));
        TicketStateMachine stateMachine = ticketStateMachine(ticketStateMachineContext.getWorkflow(),
                ticketStateMachineContext.getSchema(),
                ticketStateMachineContext.getTicketDetails(),
                ticketStateMachineContext.getSubject());
        stateMachine.trigger(new Trigger(source, payload), TriggerStrategy.EXECUTE_ALL);
        return Optional.of(stateMachine.getTicket());
    }

    private TicketStateMachine ticketStateMachine(Workflow workflow, Schema schema, TicketDetails ticketDetails, SubjectSummary subject) {
        return new TicketStateMachine(
                workflow.getTicketStateTransitions(),
                schema,
                ticketDetails,
                mapper,
                ruleEngine,
                new TransitionHandler() {

                    @Override
                    public TicketDetails beforeTransition(TicketStateTransition transition, TicketDetails ticket, Trigger event) {
                        log.info("Ticket {} beforeTransition is now in state: {}",
                                ticket.getSummary().getId(),
                                ticket.getSummary().getTicketState().getDisplayName());
                        return ticket;
                    }

                    @Override
                    public TicketDetails onTransition(TicketStateTransition transition, TicketDetails ticket, Trigger event) {
                        val updatedTicket = Objects.requireNonNull(updateTicketState(workflow, ticket, transition)
                                .map(t -> ticketDetails(t, workflow, subject))
                                .orElse(null));
                        log.info("Ticket {} onTransitions is now in state: {}",
                                ticket.getSummary().getId(),
                                ticket.getSummary().getTicketState().getDisplayName());
                        return updatedTicket;
                    }

                    @Override
                    public TicketDetails afterTransition(TicketStateTransition transition, TicketDetails ticket, Trigger event) {
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
                            updatedTicket = readDetails(workflow, subject, ticketId);
                        }
                        return updatedTicket;
                    }
                });
    }

    private void idempotencyCheckIfAny(TicketStrategy ticketStrategy, TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStrategy.subjectIdempotencyStrategy) {
            case IGNORE_CHECK:
                break;
            case CHECK_FOR_SUBJECT:
                addTicketIfExists(ticketStateMachineContext);

        }
    }

    private void addSubject(TicketStrategy ticketStrategy, JsonNode payload, TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStrategy.subjectStrategy) {
            case GET_FROM_PROVIDED_ID :
                subjectFromData(ticketStateMachineContext, payload);
                break;
            case EXTRACT_FROM_RAW_DATA:
                subjectFromRawData(ticketStateMachineContext, payload);
                break;
            case GET_FROM_TICKET :
                subjectFromTicket(ticketStateMachineContext);
                break;
        }
    }

    private void addSchema(TicketStrategy ticketStrategy, TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStrategy.schemaStrategy) {
            case GET_FROM_WORKFLOW:
                schemaFromWorkflow(ticketStateMachineContext);
                break;
        }
    }

    private void addWorkflow(TicketStrategy ticketStrategy, JsonNode payload, TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStrategy.workflowStrategy) {
            case GET_FROM_PROVIDED_ID:
                workflowFomId(ticketStateMachineContext, payload);
                break;
            case SELECT_WORKFLOW :
                workflowFromSelection(ticketStateMachineContext, payload);
                break;
            case GET_FROM_TICKET :
                workflowFomTicket(ticketStateMachineContext);
            break;
        }
    }

    private void addTicketSkeleton(TicketStrategy ticketStrategy, JsonNode payload, TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStrategy){
            case GET_FROM_PROVIDED_ID:
                ticketSkeletonFromId(ticketStateMachineContext, payload);
                break;
            case GET_FROM_EXTRACTED_ID:
                ticketSkeletonFromId(ticketStateMachineContext, payload);
                break;
            case CREATE_FROM_RAW_DATA:
                break;
            case CREATE_FROM_GIVEN_DATA:
                break;
        }
    }

    private void createNewTicket(JsonNode payload, TicketStateMachineContext ticketStateMachineContext, SchemaOpValidationResult<List<TicketFieldData>> fieldMappingResult) {
        ticketStore.create(UUID.randomUUID().toString(),
                templateEngine.evaluateToText(
                                ticketStateMachineContext.getWorkflow().getTitleTemplate(),
                                payload)
                        .orElse("Default"),
                templateEngine.evaluateToText(
                        ticketStateMachineContext.getWorkflow().getDescriptionTemplate(),
                        payload).orElse(""),
                ticketStateMachineContext.getWorkflow().getId(),
                ticketStateMachineContext.getSubject().getGlobalId(),
                ticketStateMachineContext.getWorkflow().getStartStateId(),
                TicketPriority.MEDIUM,
                Objects.requireNonNullElse(
                        fieldMappingResult.getData(),
                        List.of()))
                .ifPresent(ticketStateMachineContext::setTicketSkeleton);
    }

    private void addTicketIfExists(TicketStateMachineContext ticketStateMachineContext) {
        if(ticketStateMachineContext.getTicketSkeleton() != null) {
            return;
        }
        val terminalStates = findTerminalStates(ticketStateMachineContext.getWorkflow());
        ticketStore.list(List.of(new TicketWorkflowEquals(ticketStateMachineContext.getWorkflow().getId()),
                                new TicketSubjectEquals(ticketStateMachineContext.getSubject().getGlobalId()),
                                new TicketStateIn(terminalStates, true)),
                        List.of(),
                        null,
                        1,
                        Map.of())
                .getResults()
                .stream()
                .findAny()
                .ifPresent(ticketStateMachineContext::setTicketSkeleton);
    }

    private void subjectFromData(TicketStateMachineContext ticketStateMachineContext, JsonNode payload) {
        if(ticketStateMachineContext.getSubject() != null) {
            return;
        }
        //TODO: Move to fixed template for console data
        val subjectId = SubjectID.builder()
                .type(SubjectIDType.valueOf(payload.get("subjectType").asText()))
                .subType(payload.get("subjectSubType").asText())
                .value(payload.get(SUBJECT_ID).asText())
                .build();
        val subject = Objects.requireNonNull(fetchSubject(subjectId).orElseGet(() -> createEmptySubject(subjectId)));
        ticketStateMachineContext.setSubject(subject);
    }

    private void subjectFromRawData(TicketStateMachineContext ticketStateMachineContext, JsonNode payload) {
        if(ticketStateMachineContext.getSubject() != null) {
            return;
        }
        val subjectId =  extractSubjectId(payload, ticketStateMachineContext.getWorkflow());
        val subject = Objects.requireNonNull(fetchSubject(subjectId).orElseGet(() -> createEmptySubject(subjectId)));
        ticketStateMachineContext.setSubject(subject);
    }

    private void subjectFromTicket(TicketStateMachineContext ticketStateMachineContext) {
        if(ticketStateMachineContext.getSubject() != null) {
            return;
        }
        val ticket = ticketStateMachineContext.getTicketSkeleton();
        val subjectId = ticket.getSubjectId();
        val subject =  subjectStore.getSubjectSummary(subjectId).orElse(null);
        ensureNonNull(subject, ConductorErrorCode.TICKET_MGMT_NO_SUBJECT,
                Map.of(SUBJECT_ID, subjectId));
        ticketStateMachineContext.setSubject(subject);
    }


    private void schemaFromWorkflow(TicketStateMachineContext ticketStateMachineContext){
        if(ticketStateMachineContext.getSchema() != null) {
            return;
        }
        val workflow = ticketStateMachineContext.getWorkflow();
        val workflowId = workflow.getId();
        val schemaId = workflow.getSchemaId();
        val schema =  schemaStore.get(schemaId).orElse(null);
        ensureNonNull(schema, ConductorErrorCode.TICKET_MGMT_NO_SCHEMA,
                Map.of(TICKET_ID, ticketStateMachineContext.getTicketSkeleton() != null
                        ? ticketStateMachineContext.getTicketSkeleton().getTicketId() : "NA",
                        WORKFLOW_ID, workflowId,
                        SCHEMA_ID, schemaId));
        ticketStateMachineContext.setSchema(schema);
    }

    private void workflowFomId(TicketStateMachineContext ticketStateMachineContext, JsonNode payload){
        if(ticketStateMachineContext.getWorkflow() != null) {
            return;
        }
        val workflowId = payload.get(WORKFLOW_ID).asText();
        val workflow = workflowStore.read(workflowId)
                .orElse(null);
        ensureNonNull(workflow, ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW,
                Map.of(TICKET_ID, ticketStateMachineContext.getTicketSkeleton() != null
                        ? ticketStateMachineContext.getTicketSkeleton().getTicketId() : "NA",
                        WORKFLOW_ID, workflowId));
        ticketStateMachineContext.setWorkflow(workflow);
    }

    private void workflowFromSelection(TicketStateMachineContext ticketStateMachineContext, JsonNode payload) {
        if(ticketStateMachineContext.getWorkflow() != null) {
            return;
        }
        ticketStateMachineContext.setWorkflow(selectWorkflow(payload));
    }

    private void workflowFomTicket(TicketStateMachineContext ticketStateMachineContext){
        if(ticketStateMachineContext.getWorkflow() != null) {
            return;
        }
        TicketSkeleton ticketSkeleton = ticketStateMachineContext.getTicketSkeleton();
        val ticketId = ticketSkeleton.getTicketId();
        val workflowId = ticketSkeleton.getWorkflowId();
        val workflow = workflowStore.read(workflowId).orElse(null);
        ensureNonNull(workflow, ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW,
                Map.of(TICKET_ID, ticketId,
                        WORKFLOW_ID, workflowId));
        ticketStateMachineContext.setWorkflow(workflow);
    }

    private void ticketSkeletonFromId(TicketStateMachineContext ticketStateMachineContext, JsonNode payload){
        if(ticketStateMachineContext.getTicketDetails() != null) {
            return;
        }
        val ticketId = payload.get(TICKET_ID).asText();
        val ticket = ticketStore.read(ticketId, true)
                .orElse(null);
        ensureNonNull(ticket, ConductorErrorCode.TICKET_MGMT_NO_TICKET,
                Map.of(TICKET_ID, ticketId));
        ticketStateMachineContext.setTicketSkeleton(ticket);
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
        return processTicketUpdate(payload, schema, workflow, subject.getSummary(), (node, fields) -> {}, Trigger.TriggerSource.INGRESS_CALLBACK);
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
            Trigger.TriggerSource source) {
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
            Trigger.TriggerSource source) {
        TicketStateMachine stateMachine = ticketStateMachine(workflow, schema, ticket, subject);
        stateMachine.trigger(new Trigger(source, payload), TriggerStrategy.EXECUTE_ALL);
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
