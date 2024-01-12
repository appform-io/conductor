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

import com.google.common.annotations.VisibleForTesting;
import io.appform.conductor.model.workflow.*;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.StreamSupport;

/**
 *
 */
@Slf4j
public class CachingWorkflowStore implements WorkflowStore {

    private final WorkflowStore root;

    private Cache<String, Workflow> cache;

    private final AtomicBoolean initialized = new AtomicBoolean();

    @Inject
    public CachingWorkflowStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) WorkflowStore root,
            HazelcastClient hazelcastClient) {
        this.root = root;
        hazelcastClient.registerInitializer(cacheManager -> {
            val config = new MutableConfiguration<String, Workflow>()
                    /*.setCacheLoaderFactory(() -> new CacheLoader<>() {
                        @Override
                        public Workflow load(String key) throws CacheLoaderException {
                            return root.read(key).orElse(null);
                        }

                        @Override
                        public Map<String, Workflow> loadAll(Iterable<? extends String> iterable) throws CacheLoaderException {
                            return root.list(EnumSet.allOf(WorkflowState.class))
                                    .stream()
                                    .collect(Collectors.toMap(Workflow::getId, Function.identity()));
                        }
                    })*/
                    .setExpiryPolicyFactory(EternalExpiryPolicy::new)
                    .setReadThrough(false)
                    .setWriteThrough(false)
                    .setStatisticsEnabled(true);
            cache = cacheManager.createCache(getClass().getSimpleName(), config);
            root.list(EnumSet.allOf(WorkflowState.class)).forEach(wf -> cache.put(wf.getId(), wf));
            initialized.set(true);
            log.info("Cache created");
        });

    }

    @VisibleForTesting
    boolean isInitialized() {
        return initialized.get();
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
    public Optional<Workflow> read(String workflowId) {
        return Optional.of(cache.get(workflowId));
    }

    @Override
    public Optional<Workflow> update(String id, UnaryOperator<Workflow> updater) {
        return root.update(id, updater)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    private Workflow refreshworkflow(String id, Workflow wf) {
        cache.put(id, wf); //Remove old value
        return cache.get(id);
    }

    @Override
    public List<Workflow> list(Set<WorkflowState> desiredState) {
        return StreamSupport.stream(cache.spliterator(), false)
                .map(Cache.Entry::getValue)
                .filter(wf -> desiredState.contains(wf.getState()))
                .toList();
    }

    @Override
    public boolean deleteWorkflow(String id) {
        val status = root.deleteWorkflow(id);
        if (status) {
            cache.remove(id);
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
    public Optional<Workflow> deleteState(String workflowId, String stateId) {
        return root.deleteState(workflowId, stateId)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
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
    public Optional<Workflow> deleteTransition(String workflowId, String transitionId) {
        return root.deleteTransition(workflowId, transitionId)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    public Optional<Workflow> createOrUpdateSelectionRule(
            String workFlowId,
            String ruleId,
            Rule rule) {
        return root.createOrUpdateSelectionRule(workFlowId, ruleId, rule)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }

    @Override
    public Optional<Workflow> deleteSelectionRule(String workflowId, String ruleId) {
        return root.deleteSelectionRule(workflowId, ruleId)
                .map(wf -> refreshworkflow(wf.getId(), wf));
    }
}
