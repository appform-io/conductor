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

import io.appform.conductor.model.workflow.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 *
 */
public interface WorkflowStore {
    Optional<Workflow> create(
            final String workflowId,
            final String name,
            final String description,
            final String schemaId,
            final Template titleTemplate,
            final Template descriptionTemplate,
            final Template subjectIdTemplate);

    Optional<Workflow> read(final String workflowId);

    Optional<Workflow> update(final String id, final UnaryOperator<Workflow> updater);

    List<Workflow> list(final Set<WorkflowState> desiredState);

    boolean deleteWorkflow(final String id);

    Optional<Workflow> createOrUpdateState(
            final String workflowId,
            final String stateId,
            final String displayName,
            final String description,
            final boolean terminal,
            List<String> allowedActions,
            List<String> editableFields,
            List<String> visibleFields, List<String> requiredFields);

    Optional<Workflow> deleteState(
            final String workflowId,
            final String stateId);

    Optional<Workflow> createOrUpdateTransition(
            final String workflowId,
            final String transitionId,
            final String from,
            final String to,
            final TicketStateTransition.TicketStateTransitionType type,
            final Rule rule,
            final List<String> actionIds);

    Optional<Workflow> deleteTransition(
            final String workflowId,
            final String transitionId);

    Optional<Workflow> createOrUpdateSelectionRule(
            final String workFlowId,
            final String ruleId,
            final Rule rule);

    Optional<Workflow> deleteSelectionRule(
            final String workflowId,
            final String ruleId);

}
