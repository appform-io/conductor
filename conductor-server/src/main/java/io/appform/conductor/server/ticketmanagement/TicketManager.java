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
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import io.appform.conductor.model.actions.ActionExecutionResult;
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
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.filter.Filters;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.TicketFilterType;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketStateIn;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketSubjectEquals;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketWorkflowEquals;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.ruleengines.RuleEngine;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.templateengines.TemplateEngine;
import io.appform.conductor.server.ticketmanagement.statemachine.TicketStateMachine;
import io.appform.conductor.server.ticketmanagement.statemachine.TransitionHandler;
import io.appform.conductor.server.ticketmanagement.statemachine.models.TicketStateMachineContext;
import io.appform.conductor.server.ticketmanagement.statemachine.models.TriggerData;
import io.appform.conductor.server.ticketmanagement.statemachine.models.strategy.TicketMetaDataFetchStrategy;
import io.appform.conductor.server.ticketmanagement.statemachine.models.strategy.TicketStateMachineContextBuilderStrategy;
import io.appform.conductor.server.ticketmanagement.statemachine.models.strategy.TriggerStrategy;
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
    public static final String TICKET_ID = "ticketId";
    public static final String WORKFLOW_ID = "workflowId";
    public static final String SCHEMA_ID = "schemaId";
    public static final String SUBJECT_ID = "subjectId";
    public static final String ACTION_ID = "actionId";
    public static final String STATE_ID = "stateId";
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

    public TicketListResponse ticketsForSubject(final String subjectId, String start, int size) {

        return search(List.of(TicketSubjectEquals.builder()
                                      .subjectId(subjectId)
                                      .build()),
                      List.of(),
                      start,
                      size);
    }

    @SneakyThrows
    public TicketListResponse search(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size) {
        return (TicketListResponse) query(TicketListRequest.builder()
                                                  .filters(Filters.builder()
                                                                   .ticketFilters(ticketFilters)
                                                                   .fieldFilters(fieldFilters)
                                                                   .build())
                                                  .direction(TicketListRequest.Direction.FORWARD)
                                                  .next(start)
                                                  .size(size)
                                                  .build());
    }

    @SneakyThrows
    public TicketListResponse since(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size) {
        return (TicketListResponse) query(TicketListRequest.builder()
                                                  .filters(Filters.builder()
                                                                   .ticketFilters(ticketFilters)
                                                                   .fieldFilters(fieldFilters)
                                                                   .build())
                                                  .direction(TicketListRequest.Direction.REVERSE)
                                                  .next(start)
                                                  .size(size)
                                                  .build());
    }

    public TicketQueryResponse query(TicketQueryRequest operation) {
        val ticketFilters = null == operation.getFilters() || null == operation.getFilters().ticketFilters()
                            ? List.<TicketFilter>of()
                            : operation.getFilters().ticketFilters();
        val fieldFilters = null == operation.getFilters() || null == operation.getFilters().fieldFilters()
                           ? List.<TicketFieldFilter>of()
                           : operation.getFilters().fieldFilters();
        return operation.accpet(new TicketQueryOperationVisitor<>() {
            @Override
            public TicketQueryResponse visit(TicketListRequest listRequest) {
                val fields = Objects.requireNonNullElse(listRequest.getTicketDataFields(), List.<String>of());
                val direction = Objects.requireNonNullElse(listRequest.getDirection(),
                                                                 TicketListRequest.Direction.FORWARD);
                return switch (direction) {
                    case FORWARD -> toGistList(listRequest.getQueryId(),
                                               ticketStore.list(ticketFilters,
                                                                fieldFilters,
                                                                listRequest.getNext(),
                                                                listRequest.getSize(),
                                                                relevantFieldSchema(ticketFilters),
                                                                !fields.isEmpty(),
                                                                fields));
                    case REVERSE -> toGistList(listRequest.getQueryId(),
                                               ticketStore.since(ticketFilters,
                                                                 fieldFilters,
                                                                 listRequest.getNext(),
                                                                 listRequest.getSize(),
                                                                 relevantFieldSchema(ticketFilters),
                                                                 !fields.isEmpty(),
                                                                 fields));
                };
            }

            @Override
            public TicketQueryResponse visit(TicketGroupRequest groupRequest) {
                return ticketStore.groupCount(groupRequest.getQueryId(),
                                              ticketFilters,
                                              fieldFilters,
                                              relevantFieldSchema(ticketFilters),
                                              groupRequest.getGroupingFields());
            }
        });
    }

    private TicketListResponse toGistList(
            String requestId,
            TicketSkeletonListResult result) {
        return TicketListResponse.builder()
                .requestId(requestId)
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
                                                           skel.getFields(),
                                                           skel.getCreated(),
                                                           skel.getUpdated());
                                 })
                                 .toList())
                .build();
    }

    public TicketGroupResponse groupCount(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String ticketPropertyName) {
        return (TicketGroupResponse)query(TicketGroupRequest.builder()
                                                  .filters(Filters.builder()
                                                                   .ticketFilters(ticketFilters)
                                                                   .fieldFilters(fieldFilters)
                                                                   .build())
                                                  .groupingFields(List.of(new ColumnGroupingElement(ticketPropertyName, ticketPropertyName)))
                                                  .build());
    }

    @SneakyThrows
    public Optional<TicketDetails> createTicket(
            final String title,
            final String description,
            final SubjectIDType subjectIDType,
            final String subjectIdSubType,
            final String subjectIdValue,
            final String workflowId) {
        val subjectID = mapper.createObjectNode();
        subjectID.put(SubjectID.Fields.type, subjectIDType.name());
        subjectID.put(SubjectID.Fields.subType, subjectIdSubType);
        subjectID.put(SubjectID.Fields.value, subjectIdValue);

        val payload = mapper.createObjectNode();
        payload.put(INTERNAL_TICKET_TITLE_FIELD, title);
        payload.put(INTERNAL_TICKET_DESC_FIELD, description);
        payload.put(INTERNAL_WORKFLOW_FIELD, workflowId);
        payload.set(INTERNAL_SUBJECT_FIELD, subjectID);

        return triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy.CONSOLE,
                                         payload, (node, fields, schema) -> {});
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
        return triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy.CONSOLE_UPDATE,
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
            final String description) {
        ConductorServerUtils.ensureNonNull(ticketStore.update(ticketId,
                                                              ticketSkeleton -> ticketSkeleton.setTitle(title)
                                                                      .setDescription(description),
                                                              List.of()).orElse(null),
                                           ConductorErrorCode.TICKET_MGMT_NO_TICKET,
                                           Map.of(TICKET_ID, ticketId));
        return triggerTicketStateMachine(ticketId);
    }

    @SneakyThrows
    public Optional<TicketDetails> processFormTicketPriorityUpdate(
            final String ticketId,
            final TicketPriority priority) {
        ConductorServerUtils.ensureNonNull(ticketStore.changePriority(ticketId, priority)
                                                   .orElse(null),
                                           ConductorErrorCode.TICKET_MGMT_NO_TICKET,
                                           Map.of(TICKET_ID, ticketId));
        return triggerTicketStateMachine(ticketId);
    }

    @SneakyThrows
    public Optional<TicketDetails> processCallback(final String ticketId, final JsonNode payload) {
        ((ObjectNode) payload).put(INTERNAL_TICKET_FIELD, ticketId);
        return triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy.CALLBACK,
                                         payload, (node, fields, schema) -> {
                });
    }

    public boolean assignTicketToGroup(final String ticketId, final String groupId) {
        val group = groupStore.read(groupId).orElse(null);
        val ticketCurrState = ticketStore.read(ticketId, false).orElse(null);
        if (null == group || null == ticketCurrState) {
            return false;
        }
        val existingUserId = ticketCurrState.getAssignedToUserId();
        val newUserId = !Strings.isNullOrEmpty(existingUserId)
                                && groupStore.findGroupsForUser(existingUserId).contains(group)
                        ? existingUserId
                        : null;
        return ticketStore.updateSkeleton(ticketId,
                                          ticketSkeleton -> ticketSkeleton.setAssignedToGroupId(groupId)
                                                  .setAssignedToUserId(newUserId))
                .filter(ticketSkeleton -> groupId.equals(ticketSkeleton.getAssignedToGroupId()))
                .isPresent();
    }

    public boolean assignTicketToUser(final String ticketId, final String userId) {
        val ticketCurrState = ticketStore.read(ticketId, false).orElse(null);
        if (null == ticketCurrState
                || null == ticketCurrState.getAssignedToGroupId()
                || groupStore.findGroupsForUser(userId)
                .stream()
                .map(Group::getId)
                .noneMatch(gid -> gid.equals(ticketCurrState.getAssignedToGroupId()))) {
            return false;
        }
        return userStore.getById(userId)
                .filter(userSummary -> UserState.ACTIVE.equals(userSummary.getState()))
                .flatMap(group -> ticketStore.update(ticketId,
                                                     ticketSkeleton -> ticketSkeleton.setAssignedToUserId(userId),
                                                     List.of()))
                .filter(ticketSkeleton -> userId.equals(ticketSkeleton.getAssignedToUserId()))
                .isPresent();
    }

    public boolean unassignTicketFromEveryone(final String ticketId) {
        return unassignTicketFromUser(ticketId, null);
    }

    public boolean unassignTicketFromUser(final String ticketId, final String userId) {
        return ticketStore.update(ticketId,
                                  ticketSkeleton -> ticketSkeleton.setAssignedToUserId(userId),
                                  List.of())
                .filter(ticketSkeleton -> (null == userId && null == ticketSkeleton.getAssignedToUserId())
                        || (userId != null && userId.equals(ticketSkeleton.getAssignedToUserId())))
                .isPresent();
    }

    public boolean triggerTicketAction(String ticketId, String actionId) {
        val ticket = ticketStore.read(ticketId, true)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_TICKET)
                        .context(Map.of(TICKET_ID, ticketId))
                        .build());

        val subjectId = ticket.getSubjectId();
        val subject = subjectStore.getSubject(subjectId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_SUBJECT)
                        .context(Map.of(TICKET_ID, ticketId,
                                        SUBJECT_ID, subjectId))
                        .build());

        val workflowId = ticket.getWorkflowId();
        val workflow = workflowStore.read(workflowId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW)
                        .context(Map.of(TICKET_ID, ticketId,
                                        WORKFLOW_ID, workflowId))
                        .build());

        val schemaId = workflow.getSchemaId();
        val schema = schemaStore.get(schemaId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_SCHEMA)
                        .context(Map.of(WORKFLOW_ID, workflowId,
                                        SCHEMA_ID, schemaId))
                        .build());

        val ticketDetails = ticketDetails(ticket, workflow, subject.getSummary());

        //validate action
        val ticketState = ticketDetails.getSummary().getTicketState();
        if (ticketState.getVisibleActions().stream()
                .noneMatch(eligibleAction -> eligibleAction.equals(actionId))) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.TICKET_MGMT_NO_STATE_ACTION)
                    .context(Map.of(TICKET_ID, ticketId,
                                    ACTION_ID, actionId,
                                    STATE_ID, ticketState.getId()))
                    .build();
        }

        //trigger action
        val actionExecutionData = new ActionExecutor.ActionEvalData(
                workflow,
                schema,
                ticketDetails,
                ConductorServerUtils.ticketToJsonNode(mapper, ticketDetails, schema),
                mapper.createObjectNode());

        val action = actionStore.read(actionId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_ACTION)
                        .context(Map.of(TICKET_ID, ticketId,
                                        WORKFLOW_ID, workflow.getId(),
                                        ACTION_ID, actionId))
                        .build());

        return actionExecutor.execute(action, actionExecutionData) == ActionExecutionResult.SUCCESS
                && triggerTicketStateMachine(ticketId).isPresent();
    }

    private Optional<TicketDetails> triggerTicketStateMachine(String ticketId) {
        val payload = mapper.createObjectNode();
        payload.put(INTERNAL_TICKET_FIELD, ticketId);
        return triggerTicketStateMachine(TicketStateMachineContextBuilderStrategy.CONSOLE_UPDATE,
                                         payload, (node, fields, schema) -> {
                });
    }

    private TicketSkeleton readDetails(String ticketId) {
        return ticketStore.read(ticketId, true)
                .orElse(null);
    }

    private Optional<TicketSkeleton> updateTicketFields(
            TicketSkeleton skeleton,
            List<TicketFieldData> fields) {
        return ticketStore.setFields(skeleton.getTicketId(), fields);
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
        val actionIds = Objects.requireNonNullElse(skeleton.getTicketActionsIds(), List.<String>of());
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
                                 actionIds.isEmpty()
                                 ? List.of()
                                 : actionStore.listActionsForIds(actionIds));
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


    private Optional<TicketDetails> triggerTicketStateMachine(
            TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
            JsonNode payload,
            TriConsumer<JsonNode, List<TicketFieldData>, Schema> mappedFieldDecorator) {
        val ticketStateMachineContext = new TicketStateMachineContext();
        addExistingTicketToContext(ticketStateMachineContextBuilderStrategy, payload, ticketStateMachineContext);
        addWorkflowToContext(ticketStateMachineContextBuilderStrategy, payload, ticketStateMachineContext);
        addSchemaToContext(ticketStateMachineContextBuilderStrategy, ticketStateMachineContext);
        addSubjectToContext(ticketStateMachineContextBuilderStrategy, payload, ticketStateMachineContext);
        checkDuplicateTicketAndAddToContext(ticketStateMachineContextBuilderStrategy, ticketStateMachineContext);
        addTicketFieldMappingToContext(payload, ticketStateMachineContext, mappedFieldDecorator);
        createOrUpdateTicketAndAddToContext(ticketStateMachineContextBuilderStrategy,
                                            payload,
                                            ticketStateMachineContext);

        if (ticketStateMachineContext.currentState().isTerminal()) {
            switch (ticketStateMachineContextBuilderStrategy.getTicketTerminalStateStrategy()) {
                case ABORT:
                    return Optional.of(ConductorServerUtils.ticketDetails(ticketStateMachineContext));
                case CREATE_NEW:
                    createNewTicketAndAddToContext(ticketStateMachineContextBuilderStrategy
                                                           .getTicketMetaDataFetchStrategy(),
                                                   payload, ticketStateMachineContext);
            }
        }

        ticketStateMachineContext.setTicketAssignedToGroup(assignedToGroup(ticketStateMachineContext
                                                                                   .getTicketSkeleton()))
                .setTicketAssignedToUser(assignedToUser(ticketStateMachineContext
                                                                .getTicketSkeleton()))
                .setTicketCreatedBy(userSummary(ticketStateMachineContext
                                                        .getTicketSkeleton().getCreatedByUserId()));

        val stateMachine = ticketStateMachine(ticketStateMachineContext);
        stateMachine.trigger(new TriggerData(payload), TriggerStrategy.EXECUTE_ALL);

        return Optional.of(ConductorServerUtils.ticketDetails(ticketStateMachineContext));
    }

    private void validateTicketFieldUpdate(TicketState ticketState, List<TicketFieldData> editedFields) {
        val mutatedTicketFields = editedFields.stream().map(TicketFieldData::getSchemaFieldId)
                .collect(Collectors.toSet());
        val eligibleEditableTicketFields = new HashSet<>(ticketState.getEditableFields());
        val additionalMutatedFields = Sets.difference(mutatedTicketFields, eligibleEditableTicketFields);
        if (!additionalMutatedFields.isEmpty()) {
            val stateId = ticketState.getId();
            log.error("Updating non mutable fields:{} were updated in state:{}",
                      additionalMutatedFields, stateId);
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.TICKET_MGMT_NON_EDITABLE_FIELDS_UPDATED)
                    .context(Map.of(STATE_ID, stateId))
                    .build();
        }

    }

    private TicketStateMachine ticketStateMachine(TicketStateMachineContext ticketStateMachineContext) {
        return new TicketStateMachine(
                ticketStateMachineContext,
                mapper,
                ruleEngine,
                new TransitionHandler() {

                    @Override
                    public void beforeTransition(
                            TicketStateTransition transition,
                            TicketStateMachineContext context,
                            TriggerData event) {
                        val ticketId = context.ticketId();
                        val ticketState = context.currentState();
                        log.info("Ticket {} beforeTransition is now in state: {}", ticketId,
                                 ticketState);
                        val stateId = ticketState.getId();
                        val ticketFields = context.getTicketSkeleton().getFields();
                        val mandatoryFields = new HashSet<>(ticketState.getRequiredFields());
                        val availableFields = ticketFields.stream().map(TicketField::getFieldSchemaId)
                                .collect(Collectors.toSet());
                        val missingMandatoryFields = Sets.difference(mandatoryFields, availableFields);
                        if (!missingMandatoryFields.isEmpty()) {
                            log.error("Missing mandatory fields:{} for ticket:{} in state:{}", missingMandatoryFields,
                                      ticketId, stateId);
                            throw ConductorException.builder()
                                    .errorCode(ConductorErrorCode.TICKET_MGMT_MISSING_FIELDS)
                                    .context(Map.of(TICKET_ID, ticketId,
                                                    STATE_ID, stateId))
                                    .build();
                        }
                    }

                    @Override
                    public void onTransition(
                            TicketStateTransition transition,
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
                    public void afterTransition(
                            TicketStateTransition transition,
                            TicketStateMachineContext context,
                            TriggerData event) {
                        val ticketId = context.ticketId();
                        val workflow = context.getWorkflow();
                        val schema = context.getSchema();
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
                                            .context(Map.of(TICKET_ID, ticketId,
                                                            WORKFLOW_ID, workflow.getId(),
                                                            ACTION_ID, actionId))
                                            .build());
                            val result = actionExecutor.execute(action, actionExecutionData);
                            log.info("Status for action execution: {}", result);
                            //Always read the updated evalTicket data before applying new transitions
                            ticketStateMachineContext.setTicketSkeleton(readDetails(ticketId));
                        }
                    }
                });
    }

    private void createOrUpdateTicketAndAddToContext(
            TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
            JsonNode payload,
            TicketStateMachineContext ticketStateMachineContext) {
        val ticketSkeleton = ticketStateMachineContext.getTicketSkeleton();
        if (ticketSkeleton == null) {
            //create new
            createNewTicketAndAddToContext(ticketStateMachineContextBuilderStrategy.getTicketMetaDataFetchStrategy(),
                                           payload,
                                           ticketStateMachineContext);
        }
        else {
            //update existing
            val fieldData = ticketStateMachineContext.getFieldMappingResult().getData();
            val ticketState = ticketStateMachineContext.currentState();
            if (!fieldData.isEmpty() && !ticketState.isTerminal()) {
                validateTicketFieldUpdate(ticketState, fieldData);
                ticketStateMachineContext.setTicketSkeleton(
                        updateTicketFields(ticketSkeleton,
                                           fieldData)
                                .orElse(null));
            }
        }
    }

    private void addTicketFieldMappingToContext(
            JsonNode payload,
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

    private void checkDuplicateTicketAndAddToContext(
            TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
            TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getTicketSkeleton() != null) {
            return;
        }
        switch (ticketStateMachineContextBuilderStrategy.getTicektIdempotencyStrategy()) {
            case CHECK_FOR_SUBJECT -> checkForSubjectTicket(ticketStateMachineContext.getWorkflow(),
                                                            ticketStateMachineContext.getSubject())
                    .ifPresent(ticketStateMachineContext::setTicketSkeleton);
            case IGNORE -> log.info("Ignoring duplicate ticket check");
        }
    }

    private Optional<TicketSkeleton> checkForSubjectTicket(Workflow workflow, SubjectSummary subjectSummary) {
        val terminalStates = findTerminalStates(workflow);
        return ticketStore.list(List.of(new TicketWorkflowEquals(workflow.getId()),
                                        new TicketSubjectEquals(subjectSummary.getGlobalId()),
                                        new TicketStateIn(terminalStates, true)),
                                List.of(),
                                null,
                                1,
                                Map.of(),
                                false,
                                List.of())
                .getResults()
                .stream()
                .findAny();
    }

    private void createNewTicketAndAddToContext(
            TicketMetaDataFetchStrategy metaDataFetchStrategy,
            JsonNode payload,
            TicketStateMachineContext ticketStateMachineContext) {
        val workflow = ticketStateMachineContext.getWorkflow();
        val startStateId = workflow.getStartStateId();
        val startState = workflow.getStates().get(startStateId);
        val editedFields = Objects.requireNonNullElse(
                ticketStateMachineContext.getFieldMappingResult().getData(),
                List.<TicketFieldData>of());
        validateTicketFieldUpdate(startState, editedFields);
        ticketStore.create(UUID.randomUUID().toString(),
                           title(metaDataFetchStrategy, payload, ticketStateMachineContext),
                           description(metaDataFetchStrategy, payload, ticketStateMachineContext),
                           workflow.getId(),
                           ticketStateMachineContext.getSubject().getGlobalId(),
                           startStateId,
                           TicketPriority.MEDIUM,
                           editedFields)
                .ifPresent(ticketStateMachineContext::setTicketSkeleton);
    }

    private void addSubjectToContext(
            TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
            JsonNode payload,
            TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getSubject() != null) {
            return;
        }
        val subject = switch (ticketStateMachineContextBuilderStrategy.getSubjectFetchStrategy()) {
            case FROM_PROVIDED_DATA -> subjectFromProvidedData(payload);
            case FROM_RAW_DATA -> subjectFromRawData(ticketStateMachineContext.getWorkflow(), payload);
            case FROM_TICKET -> subjectFromTicket(ticketStateMachineContext.getTicketSkeleton());
        };
        ticketStateMachineContext.setSubject(subject);
    }

    private void addSchemaToContext(
            TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
            TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getSchema() != null) {
            return;
        }
        switch (ticketStateMachineContextBuilderStrategy.getSchemaFetchStrategy()) {
            case FROM_WORKFLOW -> ticketStateMachineContext.setSchema(
                    schemaFromWorkflow(ticketStateMachineContext.getWorkflow()));
        }
    }

    private void addWorkflowToContext(
            TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
            JsonNode payload,
            TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getWorkflow() != null) {
            return;
        }
        val workflow = switch (ticketStateMachineContextBuilderStrategy.getWorkflowFetchStrategy()) {
            case FROM_PROVIDED_ID -> workflowFomProvidedId(payload);
            case FROM_RULE -> workflowFromRules(payload);
            case FROM_TICKET -> workflowFomTicket(ticketStateMachineContext.getTicketSkeleton());
        };
        ticketStateMachineContext.setWorkflow(workflow);
    }

    private void addExistingTicketToContext(
            TicketStateMachineContextBuilderStrategy ticketStateMachineContextBuilderStrategy,
            JsonNode payload,
            TicketStateMachineContext ticketStateMachineContext) {
        if (ticketStateMachineContext.getTicketSkeleton() != null) {
            return;
        }
        switch (ticketStateMachineContextBuilderStrategy.getTicketFetchStrategy()) {
            case FROM_PROVIDED_ID -> ticketStateMachineContext.setTicketSkeleton(ticketFromProvidedId(payload));
            case NEW -> log.info("Skipping ticket fetch as flow defines new ticket");
            case FROM_RULE -> log.info("Skipping ticket fetch  as rule definition is missing"); //TODO: change this
        }
    }

    private String title(
            TicketMetaDataFetchStrategy ticketMetaDataStrategy,
            JsonNode payload,
            TicketStateMachineContext ticketStateMachineContext) {
        return switch (ticketMetaDataStrategy) {
            case FROM_PROVIDED_DATA -> payload.get(INTERNAL_TICKET_TITLE_FIELD).asText();
            case FROM_TEMPLATE -> templateEngine.evaluateToText(
                            ticketStateMachineContext.getWorkflow().getTitleTemplate(),
                            payload)
                    .orElse("Default");
            case FROM_TICKET -> ticketStateMachineContext.getTicketSkeleton() != null
                                ? ticketStateMachineContext.getTicketSkeleton().getTitle() :
                                "Default";
        };
    }

    private String description(
            TicketMetaDataFetchStrategy ticketMetaDataFetchStrategy,
            JsonNode payload,
            TicketStateMachineContext ticketStateMachineContext) {
        return switch (ticketMetaDataFetchStrategy) {
            case FROM_PROVIDED_DATA -> payload.get(INTERNAL_TICKET_DESC_FIELD).asText();
            case FROM_TEMPLATE -> templateEngine.evaluateToText(
                    ticketStateMachineContext.getWorkflow().getDescriptionTemplate(),
                    payload).orElse("");
            case FROM_TICKET -> ticketStateMachineContext.getTicketSkeleton() != null
                                ? ticketStateMachineContext.getTicketSkeleton().getDescription() :
                                "";
        };
    }

    private SubjectSummary subjectFromProvidedData(JsonNode payload) {
        val subjectId = mapper.convertValue(payload.get(INTERNAL_SUBJECT_FIELD),
                                            SubjectID.class);
        return Objects.requireNonNull(fetchSubject(subjectId).orElseGet(() -> createEmptySubject(subjectId)));
    }

    private SubjectSummary subjectFromRawData(Workflow workflow, JsonNode payload) {
        val subjectId = extractSubjectId(payload, workflow);
        return Objects.requireNonNull(fetchSubject(subjectId)
                                              .orElseGet(() -> createEmptySubject(subjectId)));
    }

    private SubjectSummary subjectFromTicket(TicketSkeleton ticket) {
        val subjectId = ticket.getSubjectId();
        val subject = subjectStore.getSubjectSummary(subjectId).orElse(null);
        ConductorServerUtils.ensureNonNull(subject, ConductorErrorCode.TICKET_MGMT_NO_SUBJECT,
                                           Map.of(SUBJECT_ID, subjectId));
        return subject;
    }


    private Schema schemaFromWorkflow(Workflow workflow) {
        val workflowId = workflow.getId();
        val schemaId = workflow.getSchemaId();
        val schema = schemaStore.get(schemaId).orElse(null);
        ConductorServerUtils.ensureNonNull(schema, ConductorErrorCode.TICKET_MGMT_NO_SCHEMA,
                                           Map.of(WORKFLOW_ID, workflowId,
                                                  SCHEMA_ID, schemaId));
        return schema;
    }

    private Workflow workflowFomProvidedId(JsonNode payload) {
        val workflowId = payload.get(INTERNAL_WORKFLOW_FIELD).asText();
        val workflow = workflowStore.read(workflowId)
                .orElse(null);
        ConductorServerUtils.ensureNonNull(workflow, ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW,
                                           Map.of(WORKFLOW_ID, workflowId));
        return workflow;
    }

    private Workflow workflowFromRules(JsonNode payload) {
        return workflowSelector.findWorkflow(payload)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW)
                        .build());
    }

    private Workflow workflowFomTicket(TicketSkeleton ticketSkeleton) {
        val ticketId = ticketSkeleton.getTicketId();
        val workflowId = ticketSkeleton.getWorkflowId();
        val workflow = workflowStore.read(workflowId).orElse(null);
        ConductorServerUtils.ensureNonNull(workflow, ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW,
                                           Map.of(TICKET_ID, ticketId,
                                                  WORKFLOW_ID, workflowId));
        return workflow;
    }

    private TicketSkeleton ticketFromProvidedId(JsonNode payload) {
        val ticketId = payload.get(INTERNAL_TICKET_FIELD).asText();
        val ticket = ticketStore.read(ticketId, true)
                .orElse(null);
        ConductorServerUtils.ensureNonNull(ticket,
                                           ConductorErrorCode.TICKET_MGMT_NO_TICKET,
                                           Map.of(TICKET_ID, ticketId));
        return ticket;
    }

    private Map<String, FieldSchema> relevantFieldSchema(List<TicketFilter> ticketFilters) {
        return ticketFilters.stream()
                .filter(ticketFilter -> ticketFilter.getType().equals(TicketFilterType.WORKFLOW_EQUALS))
                .map(TicketWorkflowEquals.class::cast)
                .map(TicketWorkflowEquals::getWorkflowId)
                .map(workflowStore::read)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Workflow::getSchemaId)
                .map(schemaStore::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Schema::getFields)
                .flatMap(List::stream)
                .collect(Collectors.toMap(FieldSchema::getId, Function.identity()));
    }
}
