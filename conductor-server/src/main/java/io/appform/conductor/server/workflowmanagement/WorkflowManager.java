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

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.workflow.*;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.RequiredArgsConstructor;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.BiFunction;

import static io.appform.conductor.server.utils.ConductorServerUtils.ensure;
import static io.appform.conductor.server.utils.ConductorServerUtils.readableId;

/**
 * Manages lifecycle of workflows
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class WorkflowManager {

    private final WorkflowStore workflowStore;
    private final SchemaStore schemaStore;

    public Optional<Workflow> create(
            final String name,
            final String description,
            final String schemaId,
            final Template titleTemplate,
            final Template descriptionTemplate,
            final Template subjectIdTemplate) {
        val schema = schemaStore.get(schemaId)
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
        return workflowStore.update(workflowId, wf -> wf.setState(WorkflowState.ACTIVE));
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

    public Optional<Workflow> createState(
            final String workflowId,
            final String displayName,
            final String description,
            final boolean terminal,
            List<String> allowedActions,
            List<String> editableFields,
            List<String> visibleFields) {
        val stateId = workflowId + "_" + readableId(displayName);
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
                                                        visibleFields);
        ensure(updated.filter(workflow -> workflow.getStates().containsKey(stateId)).isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR, "State " + stateId + " could not be added");
        return updated;
    }

    public Optional<Workflow> updateState(
            final String workflowId,
            final String stateId,
            final String description,
            final boolean terminal,
            List<String> allowedActions,
            List<String> editableFields,
            List<String> visibleFields) {
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
                                                        visibleFields);
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
                       .noneMatch(transitions -> transitions.stream()
                               .noneMatch(ticketStateTransition -> ticketStateTransition.getTo().equals(stateId))),
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
        val wf = workflowStore.read(workflowId).orElse(null);
        ensureNotNull(workflowId, wf);
        val ruleId = UUID.randomUUID().toString();
        val updated = workflowStore.createOrUpdateSelectionRule(workflowId, ruleId, rule);
        ensure(updated.filter(workflow -> workflow.getSelectionRules().containsKey(ruleId)).isPresent(),
               ConductorErrorCode.WORKFLOW_ERROR,
               "Rule could not be added");
        return updated;
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
}
