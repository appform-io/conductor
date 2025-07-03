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

import com.google.common.collect.Sets;
import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.tasks.*;
import io.appform.conductor.model.workflow.*;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.taskmanagement.ConductorTaskScheduler;
import io.appform.conductor.server.taskmanagement.TaskStore;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.Pair;
import lombok.RequiredArgsConstructor;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 * Manages lifecycle of workflows
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class WorkflowManager {

    private final WorkflowStore workflowStore;
    private final SchemaStore schemaStore;
    private final ActionStore actionStore;
    private final TaskStore taskStore;
    private final ConductorTaskScheduler taskScheduler;
    private final TicketStore ticketStore;

    public Optional<Workflow> create(
            final String name,
            final String description,
            final String schemaId,
            final Template titleTemplate,
            final Template descriptionTemplate,
            final Template subjectIdTemplate) {
        val schema = schemaStore.read(schemaId)
                .filter(s -> s.getState().equals(SchemaState.ACTIVE))
                .orElse(null);
        ConductorServerUtils.notNull(schema,
                                     ConductorErrorCode.WORKFLOW_ERROR_INVALID_ID,
                                     "No active schema found for: " + schemaId);
        val workflowId = readableId(name);
        return workflowStore.create(workflowId,
                                    name,
                                    description,
                                    schemaId,
                                    titleTemplate,
                                    descriptionTemplate,
                                    subjectIdTemplate);
    }

    public Optional<Workflow> read(final String workflowId) {
        return workflowStore.read(workflowId);
    }

    public Optional<Workflow> activate(final String workflowId) {
        final var updated = workflowStore.update(workflowId, wf -> wf.setState(WorkflowState.ACTIVE));
        if(updated.isPresent()) {
            val schemaId = updated.map(Workflow::getSchemaId).orElse(null);
            schemaStore.updateState(schemaId, SchemaState.ACTIVE);
        }
        return updated;
    }

    public Optional<Workflow> deactivate(final String workflowId) {
        return workflowStore.update(workflowId, wf -> wf.setState(WorkflowState.INACTIVE));
    }

    public Optional<Workflow> updateDescription(final String workflowId, final String description) {
        return workflowStore.update(workflowId, wf -> wf.setDescription(description));
    }

    public Optional<Workflow> updateTemplates(
            final String workflowId,
            final Template titleTemplate,
            final Template descriptionTemplate,
            final Template subjectIdTemplate) {
        return workflowStore.update(workflowId, wf -> wf.setTitleTemplate(titleTemplate)
                .setDescriptionTemplate(descriptionTemplate)
                .setSubjectIdTemplate(subjectIdTemplate));
    }

    public Optional<Pair<Workflow, String>> createState(
            final String workflowId,
            final String displayName,
            final String description,
            final boolean terminal,
            List<String> allowedActions,
            List<String> editableFields,
            List<String> visibleFields,
            List<String> requiredFields,
            List<String> visibleActions) {
        val stateId = ConductorServerUtils.readableId(workflowId, displayName);
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        ensure(!wf.getStates().containsKey(stateId),
               ConductorErrorCode.WORKFLOW_ERROR, "State with id " + stateId + " already exists");
        val updated = workflowStore.createOrUpdateState(workflowId,
                                                        stateId,
                                                        displayName,
                                                        description,
                                                        terminal,
                                                        allowedActions,
                                                        editableFields,
                                                        visibleFields,
                                                        requiredFields,
                                                        visibleActions);
        ensure(updated.filter(workflow -> workflow.getStates().containsKey(stateId)).isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR, "State " + stateId + " could not be added");
        return updated.map(workflow -> Pair.of(workflow, stateId));
    }

    public Optional<Workflow> updateState(
            final String workflowId,
            final String stateId,
            final String description,
            final boolean terminal,
            List<String> allowedActions,
            List<String> editableFields,
            List<String> visibleFields,
            List<String> requiredFields,
            List<String> visibleActions) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        ensure(wf.getStates().containsKey(stateId),
               ConductorErrorCode.WORKFLOW_ERROR, "State with id " + stateId + " does not exist");
        val updated = workflowStore.createOrUpdateState(workflowId,
                                                        stateId,
                                                        null,
                                                        description,
                                                        terminal,
                                                        allowedActions,
                                                        editableFields,
                                                        visibleFields,
                                                        requiredFields,
                                                        visibleActions);
        ensure(updated.filter(workflow -> workflow.getStates().containsKey(stateId)).isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR, "State " + stateId + " could not be added");
        return updated;
    }

/*    public Optional<Workflow> updateStateDescription(
            final String workflowId,
            final String stateId,
            final String description) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val existing = wf.getStates().get(stateId);
        ensure(null != existing,
               ConductorErrorCode.WORKFLOW_ERROR,
               "No state found for state id: " + stateId);
        return workflowStore.createOrUpdateState(workflowId,
                                                 stateId,
                                                 existing.getDescription(),
                                                 description,
                                                 existing.isTerminal());
    }

    public Optional<Workflow> updateStateTerminalStatus(
            final String workflowId,
            final String stateId,
            final boolean terminal) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val existing = wf.getStates().get(stateId);
        ensure(null != existing,
               ConductorErrorCode.WORKFLOW_ERROR,
               "No state found for state id: " + stateId);
        ensure(terminal || (!existing.isTerminal() && wf.getTicketStateTransitions().containsKey(stateId)),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Outbound transitions still exist for state: " + stateId + " cannot be made terminal");
        return workflowStore.createOrUpdateState(workflowId,
                                                 stateId,
                                                 existing.getDisplayName(),
                                                 existing.getDescription(),
                                                 terminal);
    }*/

    public Optional<Workflow> setupInitialStateForWorkflow(String workflowId, String stateId) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val state = wf.getStates().get(stateId);
        ConductorServerUtils.notNull(state, ConductorErrorCode.WORKFLOW_ERROR_INVALID_INITIAL_STATE,
                                     "State " + stateId + " is not associated with this workflow");
        return workflowStore.update(workflowId,
                                    workflow -> workflow.setStartStateId(stateId).setState(WorkflowState.ACTIVE));
    }

    public Optional<Workflow> deleteState(
            String workflowId,
            String stateId) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        ensure(!wf.getTicketStateTransitions().containsKey(stateId),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Outbound transitions still exist for state: " + stateId);
        ensure(wf.getTicketStateTransitions()
                       .values()
                       .stream()
                       .flatMap(List::stream)
                       .noneMatch(ticketStateTransition -> ticketStateTransition.getTo().equals(stateId)),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Inbound connections still exist into state: " + stateId);
        val updated = workflowStore.deleteState(workflowId, stateId);
        ensure(updated.filter(workflow -> !workflow.getStates().containsKey(stateId)
                               && !workflow.getTicketStateTransitions().containsKey(stateId))
                       .isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Could not delete state " + workflowId + "/" + stateId);
        return updated;
    }

    public Optional<Workflow> createOrUpdateTransition(
            String workflowId,
            String from,
            String to,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule,
            List<String> actionIds) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val fromState = wf.getStates().get(from);
        val toState = wf.getStates().get(to);

        ensure(null != fromState,
               ConductorErrorCode.WORKFLOW_ERROR,
               "No state found for ID: " + from);
        ensure(null != toState,
               ConductorErrorCode.WORKFLOW_ERROR,
               "No state found for ID: " + to);
        val transitionId = readableId(workflowId, from, to, Objects.toString(System.currentTimeMillis()));
        val updated = workflowStore.createOrUpdateTransition(workflowId,
                                                             transitionId,
                                                             from,
                                                             to,
                                                             type,
                                                             rule,
                                                             actionIds);
        ensure(updated.filter(workflow -> workflow.getTicketStateTransitions()
                               .getOrDefault(from, List.of())
                               .stream()
                               .anyMatch(ticketStateTransition -> ticketStateTransition.getId().equals(transitionId)))
                       .isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Transition could not be added to workflow " + workflowId);
        return updated;
    }

    public Optional<Workflow> updateTransition(
            String workflowId,
            String transitionId,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule,
            List<String> actionIds) {
        return workflowStore.updateTransition(workflowId, transitionId, type, rule, actionIds);
    }

    public Optional<Workflow> updateTransitionType(
            String workflowId,
            String transitionId,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule) {
        return handleTransition(
                workflowId,
                transitionId,
                (workflow, existing) -> {
                    ensure(type.equals(TicketStateTransition.TicketStateTransitionType.DEFAULT)
                                   || null != rule,
                           ConductorErrorCode.WORKFLOW_ERROR,
                           "Rule is mandatory for evaluated transitions");
                    return workflowStore.createOrUpdateTransition(workflowId,
                                                                  transitionId,
                                                                  existing.getFrom(),
                                                                  existing.getTo(),
                                                                  type,
                                                                  rule,
                                                                  existing.getActionIds());
                });
    }

    public Optional<Workflow> updateTransitionRule(
            String workflowId,
            String transitionId,
            Rule rule) {
        return handleTransition(
                workflowId,
                transitionId,
                (workflow, existing) -> {
                    ensure(existing.getType().equals(TicketStateTransition.TicketStateTransitionType.EVALUATED),
                           ConductorErrorCode.WORKFLOW_ERROR,
                           "Rules can be added only for evaluated transitions");
                    return workflowStore.createOrUpdateTransition(workflowId,
                                                                  transitionId,
                                                                  existing.getFrom(),
                                                                  existing.getTo(),
                                                                  existing.getType(),
                                                                  rule,
                                                                  existing.getActionIds());
                });

    }

    public Optional<Workflow> updateTransitionAction(
            String workflowId,
            String transitionId,
            List<String> actionIds) {
        return handleTransition(
                workflowId,
                transitionId,
                (workflow, existing) -> workflowStore.createOrUpdateTransition(workflowId,
                                                                               transitionId,
                                                                               existing.getFrom(),
                                                                               existing.getTo(),
                                                                               existing.getType(),
                                                                               existing.getRule(),
                                                                               actionIds));

    }

    public Optional<Workflow> deleteTransitionAction(
            String workflowId,
            String transitionId) {
        return handleTransition(
                workflowId,
                transitionId,
                (wf, existing) -> workflowStore.deleteTransition(workflowId, transitionId));
    }

    public Optional<Workflow> addSelectionRule(final String workflowId, final Rule rule) {
        val ruleId = ConductorServerUtils.generateRuleId();
        return addSelectionRule(workflowId, ruleId, rule);
    }

    public Optional<Workflow> updateSelectionRule(final String workflowId, final String ruleId, final Rule rule) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val updated = workflowStore.createOrUpdateSelectionRule(workflowId, ruleId, rule);
        ensure(updated.filter(workflow -> workflow.getSelectionRules().containsKey(ruleId)).isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Rule could not be updated");
        return updated;
    }

    public Optional<Workflow> deleteSelectionRule(final String workflowId, final String ruleId) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val updated = workflowStore.deleteSelectionRule(workflowId, ruleId);
        ensure(updated.filter(workflow -> workflow.getSelectionRules().containsKey(ruleId)).isEmpty(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Rule could not be deleted");
        return updated;
    }

    public Optional<Workflow> addAvailableAction(final String workflowId, final String actionId) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val updated = workflowStore.update(workflowId,
                                           workflow -> workflow.setAvailableActions(
                                                   addToList(workflow.getAvailableActions(), actionId)));
        ensure(updated.filter(workflow -> workflow.getAvailableActions().contains(actionId)).isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Action could not be added");
        return updated;
    }

    public Optional<Workflow> removeAvailableAction(final String workflowId, final String actionId) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val updated = workflowStore.update(workflowId,
                                           workflow -> workflow.setAvailableActions(
                                                   removeFromList(workflow.getAvailableActions(), actionId)));
        ensure(updated.filter(workflow -> workflow.getAvailableActions().contains(actionId)).isEmpty(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Action could not be added");
        return updated;
    }

    public WorkflowDetails workflowDetails(String workflowId) {
        val workflow = read(workflowId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.WORKFLOW_ERROR_INVALID_ID)
                        .context(Map.of("id", workflowId))
                        .build());
        val schemaId = workflow.getSchemaId();
        val schema = schemaStore.read(schemaId)
                .orElseThrow(() -> ConductorException.builder()
                        .errorCode(ConductorErrorCode.SCHEMA_ERROR_INVALID_ID)
                        .context(Map.of("id", schemaId))
                        .build());
        val workflowActionIds = Set.copyOf(workflow.getAvailableActions());
        val transitionActionsIds = workflow.getTicketStateTransitions()
                .values().stream()
                .flatMap(Collection::stream)
                .flatMap(transition -> transition.getActionIds().stream())
                .collect(Collectors.toSet());
        val tasks = taskStore.listByScopes(List.of(Scope.create(Scope.ScopeType.WORKFLOW, workflowId)));
        val taskActionIds = tasks.stream()
                .map(task -> taskActionIds(task.getSpec()))
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        val actionIds = Stream.of(workflowActionIds, transitionActionsIds, taskActionIds)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toSet());
        return new WorkflowDetails(workflow,
                                   schema,
                                   actionStore.listActionsForIds(actionIds),
                                   tasks);
    }

    public ImportWorkflowResult importWorkflow(WorkflowDetails workflowDetails,
                                               boolean forceOverwriteActions,
                                               boolean forceOverwriteTasks) {
        val actions = importActions(workflowDetails.getActions(), forceOverwriteActions);
        val tasks = importTasks(workflowDetails.getTasks(), forceOverwriteTasks);
        val schema = importSchema(workflowDetails.getSchema());
        val workflow = importWorkflow(workflowDetails.getWorkflow());
        return new ImportWorkflowResult(workflow, schema, actions, tasks);
    }


    private Optional<Workflow> handleTransition(
            String workflowId,
            String transitionId,
            BiFunction<Workflow, TicketStateTransition, Optional<Workflow>> handler) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val existing = wf.getTicketStateTransitions()
                .values()
                .stream()
                .flatMap(List::stream)
                .filter(transition -> transition.getId().equals(transitionId))
                .findFirst()
                .orElse(null);
        ensure(null != existing,
               ConductorErrorCode.WORKFLOW_ERROR,
               "No transition found for " + workflowId + "/" + transitionId);

        return handler.apply(wf, existing);
    }

    private static void ensureNotNull(String workflowId, Workflow wf) {
        if (null == wf) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.WORKFLOW_ERROR_INVALID_ID)
                    .context(Map.of("id", workflowId))
                    .build();
        }
    }

    private List<String> taskActionIds(TaskSpec taskSpec) {
        return taskSpec.accept(new TaskSpecVisitor<>() {
            @Override
            public List<String> visit(RunActionOnSelectedTicketsTaskSpec runActionOnSelectedTicketsTaskSpec) {
                return runActionOnSelectedTicketsTaskSpec.getActionIds();
            }

            @Override
            public List<String> visit(RunActionOnCQLSelectTaskSpec runActionOnCQLSelectTaskSpec) {
                return runActionOnCQLSelectTaskSpec.getActionIds();
            }
        });
    }

    private ImportResult<Workflow> importWorkflow(Workflow inWorkflow) {
        val workflowId = inWorkflow.getId();
        val existingWorkflow = workflowStore.read(workflowId);

        //Check schema, if already active
        if (existingWorkflow.filter(workflow -> workflow.getState() == WorkflowState.ACTIVE).isPresent()) {
            return new ImportResult<>(inWorkflow, false,
                                      ConductorErrorCode.WORKFLOW_ERROR_ID_ALREADY_EXISTS.name());
        }

        if (existingWorkflow.isPresent()) {
            if (ticketStore.ticketExists(workflowId)
                    || !existingWorkflow.get().getSchemaId().equals(inWorkflow.getSchemaId())) {
                return new ImportResult<>(inWorkflow, false,
                                          ConductorErrorCode.WORKFLOW_ERROR_ID_ALREADY_EXISTS.name());
            }
        }

        //Deleting existing rules, state & transitions
        existingWorkflow.ifPresent(workflow -> {
            workflow.getSelectionRules().keySet()
                    .forEach(ruleId -> workflowStore.deleteSelectionRule(workflowId, ruleId));
            workflow.getStates().keySet()
                    .forEach(stateId -> workflowStore.deleteState(workflowId, stateId));
            workflow.getTicketStateTransitions().values().stream()
                    .flatMap(Collection::stream)
                    .forEach(transition -> workflowStore.deleteTransition(workflowId, transition.getId()));
        });

        //Create INACTIVE workflow state && upsert all rules, state & transitions
        return existingWorkflow.flatMap(existing ->
                                                workflowStore.update(existing.getId(),
                                                                     workflow -> workflow.setState(WorkflowState.INACTIVE)
                                                                             .setDescription(inWorkflow.getDescription())
                                                                             .setTitleTemplate(inWorkflow.getTitleTemplate())
                                                                             .setSubjectIdTemplate(inWorkflow.getSubjectIdTemplate())
                                                                             .setStartStateId(inWorkflow.getStartStateId())
                                                                             .setDescriptionTemplate(inWorkflow.getDescriptionTemplate())))
                .or(() -> workflowStore.create(inWorkflow.getId(),
                                               inWorkflow.getDisplayName(),
                                               inWorkflow.getDescription(),
                                               inWorkflow.getSchemaId(),
                                               inWorkflow.getTitleTemplate(),
                                               inWorkflow.getSubjectIdTemplate(),
                                               inWorkflow.getDescriptionTemplate())
                        .flatMap(wf -> workflowStore.update(wf.getId(),
                                                            existing -> existing.setStartStateId(inWorkflow.getStartStateId()))))
                .map(workflow -> {
                    inWorkflow.getSelectionRules()
                            .forEach((ruleId, rule) -> addSelectionRule(workflowId, ruleId, rule));
                    inWorkflow.getStates()
                            .forEach((stateId, state) -> createState(workflowId,
                                                                     state.getDisplayName(),
                                                                     state.getDescription(),
                                                                     state.isTerminal(),
                                                                     state.getAllowedActions(),
                                                                     state.getEditableFields(),
                                                                     state.getVisibleFields(),
                                                                     state.getRequiredFields(),
                                                                     state.getVisibleActions()));
                    inWorkflow.getTicketStateTransitions()
                            .forEach((stateId, transitions) ->
                                             transitions.forEach(transition
                                                                         -> createOrUpdateTransition(workflowId,
                                                                                                     transition.getFrom(),
                                                                                                     transition.getTo(),
                                                                                                     transition.getType(),
                                                                                                     transition.getRule(),
                                                                                                     transition.getActionIds())));
                    return inWorkflow;
                })
                .map(workflow -> new ImportResult<>(workflow, true, null))
                .orElse(new ImportResult<>(inWorkflow, false, ConductorErrorCode.STORE_WRITE_ERROR.name()));
    }

    private Optional<Workflow> addSelectionRule(String workflowId, String ruleId, Rule rule) {
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val updated = workflowStore.createOrUpdateSelectionRule(workflowId, ruleId, rule);
        ensure(updated.filter(workflow -> workflow.getSelectionRules().containsKey(ruleId)).isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Rule could not be added");
        return updated;
    }

    private List<ImportResult<Action>> importActions(List<Action> actions,
                                                     boolean forceOverwriteActions) {
        val existingActions = actionStore.read(actions.stream().map(Action::getId).toList())
                .stream().collect(Collectors.toMap(Action::getId,
                                                   Function.identity()));
        return actions.stream()
                .map(action -> {
                    if (existingActions.containsKey(action.getId()) && !forceOverwriteActions) {
                        return new ImportResult<>(action, false,
                                                  ConductorErrorCode.ACTION_ALREADY_EXISTS.name());
                    }
                    return actionStore.save(action)
                            .map(saved -> new ImportResult<>(saved, true, null))
                            .orElse(new ImportResult<>(null, false, ConductorErrorCode.STORE_WRITE_ERROR.name()));
                })
                .toList();
    }

    private List<ImportResult<Task>> importTasks(List<Task> inputTasks, boolean forceOverwriteTasks) {
        val tasks = Objects.requireNonNullElse(inputTasks, List.<Task>of());
        val taskIds = tasks.stream().map(Task::getId).toList();
        val existing = taskStore.listByIds(taskIds)
                .stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        return tasks.stream()
                .map(task -> {
                    if (existing.containsKey(task.getId()) && !forceOverwriteTasks) {
                        return new ImportResult<>(task,
                                                  false,
                                                  ConductorErrorCode.TASK_ALREADY_EXISTS.name());
                    }
                    taskScheduler.scheduleNewTask(task);
                    return new ImportResult<>(task, true, null);
                })
                .toList();
    }

    private ImportResult<Schema> importSchema(Schema inSchema) {
        val schemaId = inSchema.getId();
        val existingSchema = schemaStore.read(schemaId);

        //Check schema, if already active
        if (existingSchema.filter(schema -> schema.getState() == SchemaState.ACTIVE).isPresent()) {
            return new ImportResult<>(inSchema, false,
                                      ConductorErrorCode.SCHEMA_ERROR_ID_ALREADY_EXISITS.name());
        }

        //Delete all fields
        existingSchema.ifPresent(schema -> schema.getFields().forEach(fieldSchema ->
                                                                              schemaStore.deleteField(schemaId,
                                                                                                      fieldSchema.getId())));

        //Create INACTIVE schema && upsert all fields
        return existingSchema.flatMap(existing -> schemaStore.updateDescription(existing.getId(),
                                                                                inSchema.getDescription()))
                .or(() -> schemaStore.create(inSchema.getName(), inSchema.getDescription()))
                .map(schemaSummary -> {
                    inSchema.getFields()
                            .forEach(inFieldSchema -> schemaStore.addField(schemaSummary.getId(),
                                                                           ConductorServerUtils.readableId(schemaSummary.getId(),
                                                                                                           inFieldSchema.getName()),
                                                                           inFieldSchema)
                                    .orElseThrow());
                    return schemaSummary;
                })
                .flatMap(schemaSummary -> schemaStore.read(schemaSummary.getId()))
                .map(schema -> new ImportResult<>(schema, true, null))
                .orElse(new ImportResult<>(inSchema, false, ConductorErrorCode.STORE_WRITE_ERROR.name()));
    }
}
