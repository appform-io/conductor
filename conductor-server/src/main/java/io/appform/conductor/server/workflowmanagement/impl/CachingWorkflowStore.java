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

package io.appform.conductor.server.workflowmanagement.impl;

import io.appform.conductor.model.workflow.*;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.cache.Cache;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;

/**
 * Caches workflow data based on workflow id
 */
@Slf4j
@Singleton
public class CachingWorkflowStore implements WorkflowStore {

    private final WorkflowStore root;

    private final Provider<Cache<String, Workflow>> cacheProvider;

    @Inject
    public CachingWorkflowStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) final WorkflowStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;
        val cacheName = getClass().getSimpleName();
        this.cacheProvider = hazelcastClient.getORCreateCache(
                cacheName,
                cache -> root.list(EnumSet.allOf(WorkflowState.class))
                        .forEach(wf -> cache.put(wf.getId(), wf)));
    }

    @Override
    public Optional<Workflow> create(
            String workflowId,
            String name,
            String description,
            String schemaId,
            Template titleTemplate,
            Template descriptionTemplate,
            Template subjectIdTemplate) {
        return root.create(workflowId,
                           name,
                           description,
                           schemaId,
                           titleTemplate,
                           descriptionTemplate,
                           subjectIdTemplate)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> read(String workflowId) {
        return Optional.of(cacheProvider.get().get(workflowId));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> update(String id, UnaryOperator<Workflow> updater) {
        return root.update(id, updater)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public List<Workflow> list(Set<WorkflowState> desiredState) {
        return StreamSupport.stream(cacheProvider.get().spliterator(), false)
                .map(Cache.Entry::getValue)
                .filter(wf -> desiredState.contains(wf.getState()))
                .toList();
    }

    @Override
    @MonitoredFunction
    public boolean deleteWorkflow(String id) {
        val status = root.deleteWorkflow(id);
        if (status) {
            cacheProvider.get().remove(id);
        }
        return status;
    }

    @Override
    public Optional<Workflow> createOrUpdateState(
            String workflowId,
            String stateId,
            String displayName,
            String description,
            boolean terminal,
            List<String> allowedActions,
            List<String> editableFields,
            List<String> visibleFields,
            List<String> requiredFields,
            List<String> visibleActions) {
        return root.createOrUpdateState(workflowId,
                                        stateId,
                                        displayName,
                                        description,
                                        terminal,
                                        allowedActions,
                                        editableFields,
                                        visibleFields,
                                        requiredFields,
                                        visibleActions)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> deleteState(String workflowId, String stateId) {
        return root.deleteState(workflowId, stateId)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> createOrUpdateTransition(
            String workflowId,
            String transitionId,
            String from,
            String to,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule,
            List<String> actionIds) {
        return root.createOrUpdateTransition(
                        workflowId,
                        transitionId,
                        from,
                        to,
                        type,
                        rule,
                        actionIds)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> updateTransition(
            String workflowId,
            String transitionId,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule,
            List<String> actionIds) {
        return root.updateTransition(workflowId, transitionId, type, rule, actionIds)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> deleteTransition(String workflowId, String transitionId) {
        return root.deleteTransition(workflowId, transitionId)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> createOrUpdateSelectionRule(
            String workFlowId,
            String ruleId,
            Rule rule) {
        return root.createOrUpdateSelectionRule(workFlowId, ruleId, rule)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> deleteSelectionRule(String workflowId, String ruleId) {
        return root.deleteSelectionRule(workflowId, ruleId)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    private Workflow refreshworkflow(String id, Workflow wf) {
        val cache = this.cacheProvider.get();
        cache.put(id, wf); //Remove old value
        return cache.get(id);
    }
}
