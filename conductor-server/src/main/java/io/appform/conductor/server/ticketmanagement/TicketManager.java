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
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.ruleengines.RuleEngine;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.templateengines.TemplateEngine;
import io.appform.conductor.server.ticketmanagement.statemachine.TriggerData;
import io.appform.conductor.server.ticketmanagement.statemachine.TicketStateMachine;
import io.appform.conductor.server.ticketmanagement.statemachine.TransitionHandler;
import io.appform.conductor.server.ticketmanagement.statemachine.TriggerStrategy;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.TriConsumer;
import io.appform.conductor.server.workflowmanagement.WorkflowSelector;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class TicketManager {

    public static final String INTERNAL_TICKET_FIELD = "__TICKET__";
    public static final String INTERNAL_TICKET_TITLE_FIELD = "__TICKET__TITLE__";
    public static final String INTERNAL_TICKET_DESC_FIELD = "__TICKET__DESC__";
    public static final String INTERNAL_WORKFLOW_FIELD = "__WORKFLOW__";
    public static final String INTERNAL_SUBJECT_FIELD = "__SUBJECT__";
    public static final String INTERNAL_SUBJECT_TYPE_FIELD = "__SUBJECT__TYPE__";
    public static final String INTERNAL_SUBJECT_SUB_TYPE_FIELD = "__SUBJECT__SUB__TYPE__";
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
    public Optional<TicketDetails> createTicket(
            final String title,
            final String description,
            final SubjectIDType subjectIDType,
            final String subjectIdSubType,
            final String subjectIdValue,
            final String workflowId) {
        val payload = mapper.createObjectNode();
        payload.set(INTERNAL_TICKET_TITLE_FIELD, mapper.valueToTree(title));
        payload.set(INTERNAL_TICKET_DESC_FIELD, mapper.valueToTree(description));
        payload.set(INTERNAL_SUBJECT_TYPE_FIELD, mapper.valueToTree(subjectIDType));
        payload.set(INTERNAL_SUBJECT_SUB_TYPE_FIELD, mapper.valueToTree(subjectIdSubType));
        payload.set(INTERNAL_SUBJECT_FIELD, mapper.valueToTree(subjectIdValue));
        payload.set(INTERNAL_WORKFLOW_FIELD, mapper.valueToTree(workflowId));
        return triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy.CONSOLE_UPDATE, 
                payload, (node, fields, schema) -> {
                });
    }

    @SneakyThrows
    public Optional<TicketDetails> processRaw(final JsonNode payload) {
        return triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy.RAW_DATA,
                payload, (node, fields, schema) -> {
                });
    }

    @SneakyThrows
    public Optional<TicketDetails> processFormFieldUpdate(
            final String ticketId,
            final MultivaluedMap<String, String> form) {
        val payload = mapper.createObjectNode();
        form.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("ticketField-"))
                .forEach(e -> {
                    val key = e.getKey().replaceAll("^ticketField-", "");
                    val valueList = e.getValue();
                    val value = valueList.size() == 1 ? valueList.get(0) : valueList;
                    payload.set(key, mapper.valueToTree(value));
                });
        //adding ticketId
        payload.put(INTERNAL_TICKET_FIELD, ticketId);
        return triggerTicketStateMachine( TicketStateMachineContextBuilderStrategy.RAW_DATA,
                payload, (node, fields, schema) -> {
                    val fs = schema.getFields()
                            .stream().collect(Collectors.toMap(FieldSchema::getId,
                                    Function.identity()));
                    val objectNode = (ObjectNode) node;
                    objectNode.removeAll();
                    fields.forEach(field -> {
                        val fieldSchema = fs.get(field.getSchemaFieldId());
                        objectNode.set(fieldSchema.getName(),
                                ConductorServerUtils.mapValueToJsonNode(mapper,
                                        fieldSchema,
                                        field.getValue()));
                    });
                });
    }

    @SneakyThrows
    public Optional<TicketDetails> processFormSummaryUpdate(
            final String ticketId,
            final String title,
            final String description,
            final TicketPriority priority) {
        Objects.requireNonNull(ticketStore.update(ticketId,
                        ticketSkeleton -> ticketSkeleton.setTitle(title)
                                .setDescription(description)
                                .setPriority(priority),
                        List.of())
                .orElse(null));
        val payload = mapper.createObjectNode();
        payload.put(INTERNAL_TICKET_FIELD, ticketId);
        return triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy.CALLBACK,
                payload, (node, fields, schema) -> {
                });
    }

    @SneakyThrows
    public Optional<TicketDetails> processCallback(final String ticketId, final JsonNode payload) {
        ((ObjectNode) payload).put(INTERNAL_TICKET_FIELD, ticketId);
        return triggerTicketStateMachine( TicketStateMachineContextBuilderStrategy.CALLBACK, 
                payload, (node, fields, schema) -> {
                });
    }

    public boolean assignTicketToGroup(final String ticketId, final String groupId) {
        return groupStore.read(groupId)
                .flatMap(group -> ticketStore.update(ticketId,
                        ticketSkeleton -> ticketSkeleton.setAssignedToGroupId(groupId),
                        List.of()))
                .filter(ticketSkeleton -> groupId.equals(ticketSkeleton.getAssignedToGroupId()))
                .isPresent();
    }

    private TicketSkeleton readDetails(Workflow workflow, SubjectSummary subject, String ticketId) {
        return ticketStore.read(ticketId, true)
                .orElse(null);
    }

    private Optional<TicketSkeleton> updateTicketFields(TicketSkeleton skeleton,
            List<TicketFieldData> fields) {
        return ticketStore.update(skeleton.getTicketId(), t -> t, fields);
    }

    private Optional<TicketSkeleton> updateTicketState(
            Workflow workflow,
            TicketSkeleton ticketSkeleton,
            TicketStateTransition finalMatchingTransition) {
        return ticketStore.updateState(ticketSkeleton.getTicketId(),
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

    private TicketDetails ticketDetails(
            final TicketSkeleton skeleton,
            final Workflow workflow,
            SubjectSummary subject) {
        return new TicketDetails(new TicketSummary(skeleton.getTicketId(),
                skeleton.getTitle(),
                skeleton.getDescription(),
                skeleton.getWorkflowId(),
                userSummary(skeleton.getCreatedByUserId()),
                assignedToGroup(skeleton),
                assignedToUser(skeleton),
                subject,
                workflow.getStates().get(skeleton.getTicketStateId()),
                skeleton.getPriority(),
                skeleton.getCreated(),
                skeleton.getUpdated()),
                skeleton.getFields(),
                List.of()); //TODO::ACTION
    }

    private UserSummary userSummary(String userId) {
        return userStore.getById(userId)
                .orElse(null);
    }

    private UserSummary assignedToUser(TicketSkeleton skeleton) {
        return Objects.nonNull(skeleton.getAssignedToUserId())
                ? userSummary(skeleton.getAssignedToUserId())
                : null;
    }

    private Group assignedToGroup(TicketSkeleton skeleton) {
        return Objects.nonNull(skeleton.getAssignedToGroupId())
                ? groupStore.read(skeleton.getAssignedToGroupId()).orElse(null)
                : null;
    }


    private Optional<TicketDetails> triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
                                                              JsonNode payload,
                                                              TriConsumer<JsonNode, List<TicketFieldData>, Schema> mappedFieldDecorator) {
        TicketStateMachineContext ticketStateMachineContext = new TicketStateMachineContext();
        existingTicket(ticketStateMachineContextBuilderStrategy, payload, ticketStateMachineContext);
        workflow(ticketStateMachineContextBuilderStrategy, payload, ticketStateMachineContext);
        schema(ticketStateMachineContextBuilderStrategy, ticketStateMachineContext);
        subject(ticketStateMachineContextBuilderStrategy, payload, ticketStateMachineContext);
        duplicateTicket(ticketStateMachineContextBuilderStrategy, ticketStateMachineContext);
        fieldMapping(payload, ticketStateMachineContext, mappedFieldDecorator);
        createOrUpdateTicket(ticketStateMachineContextBuilderStrategy, payload, ticketStateMachineContext);

        if(ticketStateMachineContext.currentState().isTerminal()) {
            switch (ticketStateMachineContextBuilderStrategy.terminalTicketStrategy) {
                case ABORT:
                    return Optional.of(ConductorServerUtils.ticketDetails(ticketStateMachineContext));
                case CREATE_NEW:
                    createNewTicket(payload,
                            ticketStateMachineContext,
                            ticketStateMachineContextBuilderStrategy.ticketMetaDataStrategy);
            }
        }

        ticketStateMachineContext.setTicketAssignedToGroup(
                assignedToGroup(ticketStateMachineContext.getTicketSkeleton()));
        ticketStateMachineContext.setTicketAssignedToUser(
                assignedToUser(ticketStateMachineContext.getTicketSkeleton()));
        ticketStateMachineContext.setTicketCreatedBy(
                userSummary(ticketStateMachineContext.getTicketSkeleton().getCreatedByUserId()));

        TicketStateMachine stateMachine = ticketStateMachine(ticketStateMachineContext);
        stateMachine.trigger(new TriggerData(payload), TriggerStrategy.EXECUTE_ALL);

        return Optional.of(ConductorServerUtils.ticketDetails(ticketStateMachineContext));
    }

    private TicketStateMachine ticketStateMachine(TicketStateMachineContext ticketStateMachineContext) {
        return new TicketStateMachine(
                ticketStateMachineContext,
                mapper,
                ruleEngine,
                new TransitionHandler() {

                    @Override
                    public void beforeTransition(TicketStateTransition transition,
                                                                      TicketStateMachineContext context,
                                                                      TriggerData event) {
                        log.info("Ticket {} beforeTransition is now in state: {}",
                                context.ticketId(),
                                context.currentState());
                    }

                    @Override
                    public void onTransition(TicketStateTransition transition,
                                                                  TicketStateMachineContext context,
                                                                  TriggerData event) {
                        val workflow = ticketStateMachineContext.getWorkflow();
                        Objects.requireNonNull(
                                updateTicketState(workflow,
                                        ticketStateMachineContext.getTicketSkeleton(),
                                        transition)
                                .map(context::setTicketSkeleton));
                        log.info("Ticket {} onTransitions is now in state: {}",
                                context.ticketId(),
                                context.currentState());
                    }

                    @Override
                    public void afterTransition(TicketStateTransition transition,
                                                                     TicketStateMachineContext context,
                                                                     TriggerData event) {
                        val ticketId = context.ticketId();
                        val workflow = context.getWorkflow();
                        val schema = context.getSchema();
                        val subject = context.getSubject();
                        val ticketDetails = ConductorServerUtils.ticketDetails(context);
                        for (val actionId : transition.getActionIds()) {
                            val actionExecutionData = new ActionExecutor.ActionEvalData(
                                    workflow,
                                    schema,
                                    ticketDetails,
                                    ConductorServerUtils.ticketToJsonNode(mapper, ticketDetails, schema),
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
                            ticketStateMachineContext.setTicketSkeleton(readDetails(workflow, subject, ticketId));
                        }
                    }
                });
    }

    private void createOrUpdateTicket(TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
                                      JsonNode payload,
                                      TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getTicketSkeleton() == null) {
            //create new
            createNewTicket(payload,
                    ticketStateMachineContext,
                    ticketStateMachineContextBuilderStrategy.ticketMetaDataStrategy);
        } else {
            //update existing
            List<TicketFieldData> fieldData = ticketStateMachineContext.getFieldMappingResult().getData();
            if (!fieldData.isEmpty() && !ticketStateMachineContext.currentState().isTerminal()) {
                ticketStateMachineContext.setTicketSkeleton(
                        updateTicketFields(ticketStateMachineContext.getTicketSkeleton(),
                                fieldData)
                                .orElse(null));
            }
        }
    }

    private void fieldMapping(JsonNode payload,
                              TicketStateMachineContext ticketStateMachineContext,
                              TriConsumer<JsonNode, List<TicketFieldData>, Schema> mappedFieldDecorator) {
        val fieldMappingResult = 
                fieldMapper.map(ticketStateMachineContext.getSchema(), payload);
        ConductorServerUtils.ensureCondition(fieldMappingResult.getErrors().isEmpty(),
                ConductorErrorCode.TICKET_SCHEMA_VALIDATION_FAILURE,
                Map.of("errors", fieldMappingResult.getErrors()));
        mappedFieldDecorator.accept(payload, fieldMappingResult.getData(), ticketStateMachineContext.getSchema());
        ticketStateMachineContext.setFieldMappingResult(fieldMappingResult);
    }

    private void duplicateTicket(TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
                                 TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStateMachineContextBuilderStrategy.idempotencyStrategy) {
            case CHECK_FOR_SUBJECT -> checkForSubjectTicket(ticketStateMachineContext);
            case IGNORE -> log.info("Ignoring duplicate ticket check");
        }
    }

    private void checkForSubjectTicket(TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getTicketSkeleton() != null) {
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

    private void createNewTicket(JsonNode payload,
                                 TicketStateMachineContext ticketStateMachineContext,
                                 TicketMetaDataStrategy metaDataStrategy) {
        ticketStore.create(UUID.randomUUID().toString(),
                        title(metaDataStrategy, payload, ticketStateMachineContext),
                        description(metaDataStrategy, payload, ticketStateMachineContext),
                        ticketStateMachineContext.getWorkflow().getId(),
                        ticketStateMachineContext.getSubject().getGlobalId(),
                        ticketStateMachineContext.getWorkflow().getStartStateId(),
                        TicketPriority.MEDIUM,
                        Objects.requireNonNullElse(
                                ticketStateMachineContext.getFieldMappingResult().getData(),
                                List.of()))
                .ifPresent(ticketStateMachineContext::setTicketSkeleton);
    }

    private void subject(TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy, JsonNode payload, TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStateMachineContextBuilderStrategy.subjectStrategy) {
            case FROM_PROVIDED_DATA -> subjectFromProvidedData(ticketStateMachineContext, payload);
            case FROM_RAW_DATA -> subjectFromRawData(ticketStateMachineContext, payload);
            case FROM_TICKET -> subjectFromTicket(ticketStateMachineContext);
        }
    }

    private void schema(TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy, TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStateMachineContextBuilderStrategy.schemaStrategy) {
            case FROM_WORKFLOW -> schemaFromWorkflow(ticketStateMachineContext);
        }
    }

    private void workflow(TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
                          JsonNode payload, 
                          TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStateMachineContextBuilderStrategy.workflowStrategy) {
            case FROM_PROVIDED_ID -> workflowFomProvidedId(ticketStateMachineContext, payload);
            case FROM_RULE -> workflowFromRule(ticketStateMachineContext, payload);
            case FROM_TICKET -> workflowFomTicket(ticketStateMachineContext);
        }
    }

    private void existingTicket(TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
                                JsonNode payload, 
                                TicketStateMachineContext ticketStateMachineContext) {
        switch (ticketStateMachineContextBuilderStrategy.ticketStrategy) {
            case FROM_PROVIDED_ID  -> ticketFromProvidedId(ticketStateMachineContext, payload);
            case NEW -> log.info("Skipping ticket fetch as flow defines new ticket");
            case FROM_RULE -> log.info("Skipping ticket fetch  as rule defination is missing"); //TODO: change this
        }
    }

    private String title(TicketMetaDataStrategy ticketMetaDataStrategy,
                         JsonNode payload,
                         TicketStateMachineContext ticketStateMachineContext) {
        return switch (ticketMetaDataStrategy) {
            case FROM_PROVIDED_DATA -> payload.get(INTERNAL_TICKET_TITLE_FIELD).asText();
            case FROM_TEMPLATE -> templateEngine.evaluateToText(
                            ticketStateMachineContext.getWorkflow().getTitleTemplate(),
                            payload)
                    .orElse("Default");
            case FROM_TICKET -> ticketStateMachineContext.getTicketSkeleton() != null 
                                ? ticketStateMachineContext.getTicketSkeleton().getTitle():
                                  "Default";
        };
    }

    private String description(TicketMetaDataStrategy ticketMetaDataStrategy,
                               JsonNode payload,
                               TicketStateMachineContext ticketStateMachineContext) {
        return switch (ticketMetaDataStrategy) {
            case FROM_PROVIDED_DATA -> payload.get(INTERNAL_TICKET_DESC_FIELD).asText();
            case FROM_TEMPLATE -> templateEngine.evaluateToText(
                    ticketStateMachineContext.getWorkflow().getDescriptionTemplate(),
                    payload).orElse("");
            case FROM_TICKET -> ticketStateMachineContext.getTicketSkeleton() != null
                            ? ticketStateMachineContext.getTicketSkeleton().getDescription():
                            "";
        };
    }

    private void subjectFromProvidedData(TicketStateMachineContext ticketStateMachineContext,
                                         JsonNode payload) {
        if (ticketStateMachineContext.getSubject() != null) {
            return;
        }
        val subjectId = SubjectID.builder()
                .type(SubjectIDType.valueOf(payload.get(INTERNAL_SUBJECT_TYPE_FIELD).asText()))
                .subType(payload.get(INTERNAL_SUBJECT_SUB_TYPE_FIELD).asText())
                .value(payload.get(INTERNAL_SUBJECT_FIELD).asText())
                .build();
        val subject = Objects.requireNonNull(fetchSubject(subjectId).orElseGet(() -> createEmptySubject(subjectId)));
        ticketStateMachineContext.setSubject(subject);
    }

    private void subjectFromRawData(TicketStateMachineContext ticketStateMachineContext,
                                    JsonNode payload) {
        if (ticketStateMachineContext.getSubject() != null) {
            return;
        }
        val subjectId = extractSubjectId(payload, ticketStateMachineContext.getWorkflow());
        val subject = Objects.requireNonNull(fetchSubject(subjectId).orElseGet(() -> createEmptySubject(subjectId)));
        ticketStateMachineContext.setSubject(subject);
    }

    private void subjectFromTicket(TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getSubject() != null) {
            return;
        }
        val ticket = ticketStateMachineContext.getTicketSkeleton();
        val subjectId = ticket.getSubjectId();
        val subject = subjectStore.getSubjectSummary(subjectId).orElse(null);
        ConductorServerUtils.ensureNonNull(subject, ConductorErrorCode.TICKET_MGMT_NO_SUBJECT,
                Map.of(SUBJECT_ID, subjectId));
        ticketStateMachineContext.setSubject(subject);
    }


    private void schemaFromWorkflow(TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getSchema() != null) {
            return;
        }
        val workflow = ticketStateMachineContext.getWorkflow();
        val workflowId = workflow.getId();
        val schemaId = workflow.getSchemaId();
        val schema = schemaStore.get(schemaId).orElse(null);
        ConductorServerUtils.ensureNonNull(schema, ConductorErrorCode.TICKET_MGMT_NO_SCHEMA,
                Map.of(TICKET_ID, ticketStateMachineContext.getTicketSkeleton() != null
                                ? ticketStateMachineContext.getTicketSkeleton().getTicketId() : "NA",
                        WORKFLOW_ID, workflowId,
                        SCHEMA_ID, schemaId));
        ticketStateMachineContext.setSchema(schema);
    }

    private void workflowFomProvidedId(TicketStateMachineContext ticketStateMachineContext,
                                       JsonNode payload) {
        if (ticketStateMachineContext.getWorkflow() != null) {
            return;
        }
        val workflowId = payload.get(INTERNAL_WORKFLOW_FIELD).asText();
        val workflow = workflowStore.read(workflowId)
                .orElse(null);
        ConductorServerUtils.ensureNonNull(workflow, ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW,
                Map.of(TICKET_ID, ticketStateMachineContext.getTicketSkeleton() != null
                                ? ticketStateMachineContext.getTicketSkeleton().getTicketId() : "NA",
                        WORKFLOW_ID, workflowId));
        ticketStateMachineContext.setWorkflow(workflow);
    }

    private void workflowFromRule(TicketStateMachineContext ticketStateMachineContext,
                                  JsonNode payload) {
        if (ticketStateMachineContext.getWorkflow() != null) {
            return;
        }
        ticketStateMachineContext.setWorkflow(selectWorkflow(payload));
    }

    private void workflowFomTicket(TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getWorkflow() != null) {
            return;
        }
        TicketSkeleton ticketSkeleton = ticketStateMachineContext.getTicketSkeleton();
        val ticketId = ticketSkeleton.getTicketId();
        val workflowId = ticketSkeleton.getWorkflowId();
        val workflow = workflowStore.read(workflowId).orElse(null);
        ConductorServerUtils.ensureNonNull(workflow, ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW,
                Map.of(TICKET_ID, ticketId,
                        WORKFLOW_ID, workflowId));
        ticketStateMachineContext.setWorkflow(workflow);
    }

    private void ticketFromProvidedId(TicketStateMachineContext ticketStateMachineContext,
                                      JsonNode payload) {
        if (ticketStateMachineContext.getTicketSkeleton() != null) {
            return;
        }
        val ticketId = payload.get(INTERNAL_TICKET_FIELD).asText();
        val ticket = ticketStore.read(ticketId, true)
                .orElse(null);
        ConductorServerUtils.ensureNonNull(ticket, ConductorErrorCode.TICKET_MGMT_NO_TICKET,
                Map.of(TICKET_ID, ticketId));
        ticketStateMachineContext.setTicketSkeleton(ticket);
    }
}
