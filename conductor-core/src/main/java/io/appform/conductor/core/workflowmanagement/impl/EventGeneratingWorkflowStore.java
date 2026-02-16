/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.core.workflowmanagement.impl;

import io.appform.conductor.model.workflow.*;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.core.eventmanagement.EventBus;
import io.appform.conductor.model.events.impl.workflow.*;
import io.appform.conductor.core.workflowmanagement.WorkflowStore;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingWorkflowStore implements WorkflowStore {
    private final EventBus eventBus;
    private final WorkflowStore workflowStore;

    @Inject
    public EventGeneratingWorkflowStore(EventBus eventBus, @Named(Constants.CACHED_IMPLEMENTATION_NAME) WorkflowStore workflowStore) {
        this.eventBus = eventBus;
        this.workflowStore = workflowStore;
    }

    @Override
    public Optional<Workflow> create(String workflowId,
                                     String name,
                                     String description,
                                     String schemaId,
                                     Template titleTemplate,
                                     Template descriptionTemplate,
                                     Template subjectIdTemplate) {
        val res = workflowStore.create(workflowId, name, description, schemaId,
                titleTemplate, descriptionTemplate, subjectIdTemplate);
        res.ifPresent(workflow -> eventBus.publish(new WorkflowCreatedEvent(workflow.getId())));
        return res;
    }

    @Override
    public Optional<Workflow> read(String workflowId) {
        return workflowStore.read(workflowId);
    }

    @Override
    public Optional<Workflow> update(String id, UnaryOperator<Workflow> updater) {
        val res = workflowStore.update(id, updater);
        res.ifPresent(workflow -> eventBus.publish(new WorkflowUpdatedEvent(workflow.getId())));
        return res;
    }

    @Override
    public List<Workflow> list(Set<WorkflowState> desiredState) {
        return workflowStore.list(desiredState);
    }

    @Override
    public boolean deleteWorkflow(String id) {
        val res = workflowStore.deleteWorkflow(id);
        if(res) {
            eventBus.publish(new WorkflowDeletedEvent(id));
        }
        return res;
    }

    @Override
    public Optional<Workflow> createOrUpdateState(String workflowId,
                                                  String stateId,
                                                  String displayName,
                                                  String description,
                                                  boolean terminal,
                                                  List<String> allowedActions,
                                                  List<String> editableFields,
                                                  List<String> visibleFields,
                                                  List<String> requiredFields,
                                                  List<String> visibleActions) {
        val res = workflowStore.createOrUpdateState(workflowId,
                                                    stateId,
                                                    displayName,
                                                    description,
                                                    terminal,
                                                    allowedActions,
                                                    editableFields,
                                                    visibleFields,
                                                    requiredFields,
                                                    visibleActions);
        res.flatMap(workflow -> Optional.ofNullable(workflow.getStates().get(stateId)))
                .ifPresent(ticketState -> eventBus.publish(new WorkflowStateChangedEvent(workflowId, stateId)));
        return res;
    }

    @Override
    public Optional<Workflow> deleteState(String workflowId, String stateId) {
        val res = workflowStore.deleteState(workflowId, stateId);
        if(res.flatMap(workflow -> Optional.ofNullable(workflow.getStates().get(stateId))).isEmpty()) {
            eventBus.publish(new WorkflowStateDeletedEvent(workflowId, stateId));
        }
        return res;
    }

    @Override
    public Optional<Workflow> createOrUpdateTransition(String workflowId,
                                                       String transitionId,
                                                       String from,
                                                       String to,
                                                       TicketStateTransition.TicketStateTransitionType type,
                                                       Rule rule,
                                                       List<String> actionIds) {
        val res = workflowStore.createOrUpdateTransition(workflowId, transitionId,
                from, to, type, rule, actionIds);
        res.flatMap(workflow -> Optional.ofNullable(workflow.getTicketStateTransitions().get(transitionId)))
                .ifPresent(transition -> eventBus.publish(new WorkflowTransitionChangedEvent(workflowId, transitionId)));
        return res;
    }

    @Override
    public Optional<Workflow> updateTransition(
            String workflowId,
            String transitionId,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule,
            List<String> actionIds) {
        val res = workflowStore.updateTransition(workflowId, transitionId, type, rule, actionIds);
        res.flatMap(workflow -> Optional.ofNullable(workflow.getTicketStateTransitions().get(transitionId)))
                .ifPresent(transition -> eventBus.publish(new WorkflowTransitionChangedEvent(workflowId, transitionId)));
        return res;
    }

    @Override
    public Optional<Workflow> deleteTransition(String workflowId, String transitionId) {
        val res = workflowStore.deleteTransition(workflowId, transitionId);
        if(res.flatMap(workflow -> Optional.ofNullable(workflow.getTicketStateTransitions().get(transitionId))).isEmpty()) {
            eventBus.publish(new WorkflowTransitionDeletedEvent(workflowId, transitionId));
        }
        return res;
    }

    @Override
    public Optional<Workflow> createOrUpdateSelectionRule(String workFlowId, String ruleId, Rule rule) {
        val res = workflowStore.createOrUpdateSelectionRule(workFlowId, ruleId, rule);
        res.flatMap(workflow -> Optional.ofNullable(workflow.getSelectionRules().get(ruleId)))
                .ifPresent(ticketState -> eventBus.publish(new WorkflowSelectionRuleChangedEvent(workFlowId, ruleId)));
        return res;
    }

    @Override
    public Optional<Workflow> deleteSelectionRule(String workflowId, String ruleId) {
        val res = workflowStore.deleteSelectionRule(workflowId, ruleId);
        if(res.flatMap(workflow -> Optional.ofNullable(workflow.getSelectionRules().get(ruleId))).isEmpty()) {
            eventBus.publish(new WorkflowSelectionRuleDeletedEvent(workflowId, ruleId));
        }
        return res;
    }
}
