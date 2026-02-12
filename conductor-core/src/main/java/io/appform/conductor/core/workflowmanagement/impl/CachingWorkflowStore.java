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
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
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
        this.cacheProvider = hazelcastClient.loadingCache(
                cacheName,
                new CacheLoader<>() {
                    @Override
                    public Workflow load(String key) throws CacheLoaderException {
                        log.debug("Loading data for workflow {}", key);
                        return root.read(key).orElse(null);
                    }

                    @Override
                    public Map<String, Workflow> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                        val ids = StreamSupport.stream(keys.spliterator(), false)
                                .map(String.class::cast)
                                .collect(Collectors.toUnmodifiableSet());
                        log.debug("Loading schema for {}", ids);
                        return root.list(EnumSet.allOf(WorkflowState.class))
                                .stream()
                                .filter(schema -> ids.contains(schema.getId()))
                                .collect(Collectors.toUnmodifiableMap(Workflow::getId, Function.identity()));                    }
                });
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
                .flatMap(this::refreshWorkflow);
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> read(String workflowId) {
        return Optional.ofNullable(cacheProvider.get().get(workflowId));
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> update(String id, UnaryOperator<Workflow> updater) {
        return root.update(id, updater)
                .flatMap(this::refreshWorkflow);
    }

    @Override
    @MonitoredFunction
    public List<Workflow> list(Set<WorkflowState> desiredState) {
        return root.list(desiredState); //Offload to db
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
                .flatMap(this::refreshWorkflow);
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> deleteState(String workflowId, String stateId) {
        return root.deleteState(workflowId, stateId)
                .flatMap(this::refreshWorkflow);
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
                .flatMap(this::refreshWorkflow);
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
                .flatMap(this::refreshWorkflow);
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> deleteTransition(String workflowId, String transitionId) {
        return root.deleteTransition(workflowId, transitionId)
                .flatMap(this::refreshWorkflow);
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> createOrUpdateSelectionRule(
            String workFlowId,
            String ruleId,
            Rule rule) {
        return root.createOrUpdateSelectionRule(workFlowId, ruleId, rule)
                .flatMap(this::refreshWorkflow);
    }

    @Override
    @MonitoredFunction
    public Optional<Workflow> deleteSelectionRule(String workflowId, String ruleId) {
        return root.deleteSelectionRule(workflowId, ruleId)
                .flatMap(this::refreshWorkflow);
    }

    private Optional<Workflow> refreshWorkflow(Workflow workflow) {
        val cache = this.cacheProvider.get();
        log.debug("Removing data for {}", workflow.getId());
        cache.remove(workflow.getId()); //Remove old value
        return read(workflow.getId());
    }
}
