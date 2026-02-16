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

package io.appform.conductor.core.workflowmanagement.impl;

import com.google.common.base.Strings;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.workflow.*;
import io.appform.conductor.core.workflowmanagement.WorkflowStore;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * DB implementation of {@link WorkflowStore}
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class DBWorkflowStore implements WorkflowStore {

    private final LookupDao<StoredWorkflow> wfDao;
    private final RelationalDao<StoredTicketState> tsDao;
    private final RelationalDao<StoredTicketStateTransition> tstrnDao;
    private final RelationalDao<StoredWorkflowSelectionRule> wfselDao;

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredWorkflow.WORKFLOW_TABLE_NAME))
    public Optional<Workflow> create(
            @Throws.RuntimeParam("id") String workflowId,
            String name,
            String description,
            String schemaId,
            Template titleTemplate,
            Template descriptionTemplate,
            Template subjectIdTemplate) {
        wfDao.get(workflowId).ifPresent( w -> {
            log.error("CreateWorkflow failed as already exists:{}"+ workflowId);
            throw new ConductorException(ConductorErrorCode.STORE_WRITE_ERROR,
                    Map.of("type", StoredWorkflow.WORKFLOW_TABLE_NAME, "id", workflowId),
                    null );
        });
        return wfDao.save(new StoredWorkflow()
                                  .setWorkflowId(workflowId)
                                  .setDisplayName(name)
                                  .setDescription(description)
                                  .setSchemaId(schemaId)
                                  .setTitleTemplate(titleTemplate)
                                  .setDescriptionTemplate(descriptionTemplate)
                                  .setSubjectIdTemplate(subjectIdTemplate)
                                  .setState(WorkflowState.INACTIVE))
                .map(DBWorkflowStore::toWirePartial);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredWorkflow.WORKFLOW_TABLE_NAME))
    public Optional<Workflow> read(@Throws.RuntimeParam("id") String workflowId) {
        return wfDao.readOnlyExecutor(workflowId)
                .readAugmentParent(tsDao,
                                   createCriteria(StoredTicketState.class, workflowId),
                                   0,
                                   Integer.MAX_VALUE,
                                   (wf, states) -> wf.setStates(
                                           states.stream()
                                                   .collect(Collectors.toMap(StoredTicketState::getStateId,
                                                                             DBWorkflowStore::toWire))))
                .readAugmentParent(tstrnDao,
                                   createCriteria(StoredTicketStateTransition.class, workflowId),
                                   0,
                                   Integer.MAX_VALUE,
                                   (wf, states) -> wf.setTicketStateTransitions(
                                           states.stream()
                                                   .collect(Collectors.groupingBy(StoredTicketStateTransition::getFromState,
                                                                                  Collectors.mapping(DBWorkflowStore::toWire,
                                                                                                     Collectors.toList())))))
                .readAugmentParent(wfselDao,
                                   createCriteria(StoredWorkflowSelectionRule.class, workflowId),
                                   0,
                                   Integer.MAX_VALUE,
                                   (wf, states) -> wf.setRules(
                                           states.stream()
                                                   .collect(Collectors.toMap(StoredWorkflowSelectionRule::getRuleId,
                                                                             r -> new Rule(r.getRuleType(),
                                                                                           r.getRule())))))
                .execute()
                .map(wf -> new Workflow(wf.getWorkflowId(),
                                        wf.getDisplayName(),
                                        wf.getDescription(),
                                        wf.getSchemaId(),
                                        wf.getTitleTemplate(),
                                        wf.getDescriptionTemplate(),
                                        wf.getSubjectIdTemplate(),
                                        wf.getStates(),
                                        wf.getTicketStateTransitions(),
                                        wf.getAvailableActions(),
                                        wf.getStartStateId(),
                                        wf.getRules(),
                                        wf.getState(),
                                        wf.getCreated(),
                                        wf.getUpdated()));
    }


    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredWorkflow.WORKFLOW_TABLE_NAME))
    public Optional<Workflow> update(@Throws.RuntimeParam("id") String workflowId, UnaryOperator<Workflow> updater) {
        val updated = wfDao.update(workflowId, stored -> stored.map(wf -> {
                    val updatedObj = updater.apply(toWirePartial(wf));
                    return wf.setDescription(updatedObj.getDescription())
                            .setStartStateId(updatedObj.getStartStateId())
                            .setTitleTemplate(updatedObj.getTitleTemplate())
                            .setDescriptionTemplate(updatedObj.getDescriptionTemplate())
                            .setSubjectIdTemplate(updatedObj.getSubjectIdTemplate())
                            .setState(updatedObj.getState())
                            .setAvailableActions(updatedObj.getAvailableActions());
                })
                .orElse(null));
        log.info("Update for workflow {} application status: {}", workflowId, updated);
        return read(workflowId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredWorkflow.WORKFLOW_TABLE_NAME))
    public List<Workflow> list(Set<WorkflowState> desiredState) {
        //This is not very efficient, but should be behind a cache anyway
        return wfDao.scatterGather(DetachedCriteria.forClass(StoredWorkflow.class)
                                           .add(Restrictions.in(StoredWorkflow.Fields.state, desiredState)))
                .stream()
                .map(StoredWorkflow::getWorkflowId)
                .map(this::read)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredWorkflow.WORKFLOW_TABLE_NAME))
    public boolean deleteWorkflow(@Throws.RuntimeParam("id") String workflowId) {
        return wfDao.update(workflowId, stored -> stored.map(wf -> wf.setDeleted(true))
                .orElse(null));
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketState.WF_STATE_TABLE_NAME))
    public Optional<Workflow> createOrUpdateState(
            @Throws.RuntimeParam("id") String workflowId,
            @Throws.RuntimeParam("subId") String stateId,
            String displayName,
            String description,
            boolean terminal,
            List<String> allowedActions,
            List<String> editableFields,
            List<String> visibleFields,
            List<String> requiredFields,
            List<String> visibleActions) {
        val updated = wfDao.lockAndGetExecutor(workflowId)
                .createOrUpdate(tsDao,
                                createCriteria(StoredTicketState.class, workflowId, false)
                                        .add(Property.forName(StoredTicketState.Fields.stateId).eq(stateId)),
                                existing -> existing
                                        .setDescription(description)
                                        .setTerminal(terminal)
                                        .setAllowedActions(allowedActions)
                                        .setEditableFields(editableFields)
                                        .setVisibleFields(visibleFields)
                                        .setRequiredFields(requiredFields)
                                        .setVisibleActions(visibleActions)
                                        .setDeleted(false),
                                () -> new StoredTicketState()
                                        .setWorkflowId(workflowId)
                                        .setStateId(stateId)
                                        .setDisplayName(displayName)
                                        .setDescription(description)
                                        .setTerminal(terminal)
                                        .setAllowedActions(allowedActions)
                                        .setEditableFields(editableFields)
                                        .setVisibleFields(visibleFields)
                                        .setRequiredFields(requiredFields)
                                        .setVisibleActions(visibleActions))
                .execute() != null;
        log.info("State create status for {}/{}: {}", workflowId, stateId, updated);
        return read(workflowId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketState.WF_STATE_TABLE_NAME))
    public Optional<Workflow> deleteState(
            @Throws.RuntimeParam("id") String workflowId,
            @Throws.RuntimeParam("subId") String stateId) {
        val updated = wfDao.lockAndGetExecutor(workflowId)
                .update(tsDao,
                        createCriteria(StoredTicketState.class, workflowId)
                                .add(Property.forName(StoredTicketState.Fields.stateId).eq(stateId)),
                        state -> state.setDeleted(true),
                        () -> false)
                .execute();
        log.info("State delete status for {}/{}: {}", workflowId, stateId, updated);
        return read(workflowId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketStateTransition.WF_TRANSITIONS_TABLE_NAME))
    public Optional<Workflow> createOrUpdateTransition(
            @Throws.RuntimeParam("id") String workflowId,
            @Throws.RuntimeParam("subId") String transitionId,
            String from,
            String to,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule,
            List<String> actionIds) {
        val actions = actionIds.stream().filter(id -> !Strings.isNullOrEmpty(id)).toList();
        val updated = wfDao.lockAndGetExecutor(workflowId)
                .createOrUpdate(tstrnDao,
                                createCriteria(StoredTicketStateTransition.class, workflowId, false)
                                        .add(Property.forName(StoredTicketStateTransition.Fields.transitionId)
                                                     .eq(transitionId)),
                                existing -> existing
                                        .setType(type)
                                        .setRule(rule)
                                        .setActionIds(actions)
                                        .setDeleted(false),
                                () -> new StoredTicketStateTransition()
                                        .setWorkflowId(workflowId)
                                        .setTransitionId(transitionId)
                                        .setFromState(from)
                                        .setToState(to)
                                        .setType(type)
                                        .setRule(rule)
                                        .setActionIds(actions))
                .execute() != null;
        log.info("State transition create status for {}/{}: {}", workflowId, transitionId, updated);
        return read(workflowId);
    }

    @Override
    public Optional<Workflow> updateTransition(
            String workflowId,
            String transitionId,
            TicketStateTransition.TicketStateTransitionType type,
            Rule rule,
            List<String> actionIds) {
        val updated = wfDao.lockAndGetExecutor(workflowId)
                .update(tstrnDao,
                        DetachedCriteria.forClass(StoredTicketStateTransition.class)
                                .add(Property.forName(StoredTicketStateTransition.Fields.workflowId).eq(workflowId))
                                .add(Property.forName(StoredTicketStateTransition.Fields.transitionId).eq(transitionId)),
                        existing -> existing.setType(type)
                                .setRule(rule)
                                .setActionIds(actionIds),
                        () -> false)
                .execute() != null;
        log.info("State transition update status for {}/{}: {}", workflowId, transitionId, updated);
        return read(workflowId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketStateTransition.WF_TRANSITIONS_TABLE_NAME))
    public Optional<Workflow> deleteTransition(
            @Throws.RuntimeParam("id") String workflowId,
            @Throws.RuntimeParam("subId") String transitionId) {
        val updated = wfDao.lockAndGetExecutor(workflowId)
                .update(tstrnDao,
                        createCriteria(StoredTicketStateTransition.class, workflowId)
                                .add(Property.forName(StoredTicketStateTransition.Fields.transitionId).eq(transitionId)),
                        transition -> transition.setDeleted(true),
                        () -> false)
                .execute();
        log.info("State transition delete status for {}/{}: {}", workflowId, transitionId, updated);
        return read(workflowId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type",
                    value = StoredWorkflowSelectionRule.WF_SELECTION_RULE_TABLE_NAME))
    public Optional<Workflow> createOrUpdateSelectionRule(
            @Throws.RuntimeParam("id") String workflowId,
            @Throws.RuntimeParam("subId") String ruleId,
            Rule rule) {
        val updated = wfDao.lockAndGetExecutor(workflowId)
                .createOrUpdate(wfselDao,
                                createCriteria(StoredWorkflowSelectionRule.class, workflowId)
                                        .add(Property.forName(StoredWorkflowSelectionRule.Fields.ruleId).eq(ruleId)),
                                existing -> existing
                                        .setRuleType(rule.getType())
                                        .setRule(rule.getRule()),
                                () -> new StoredWorkflowSelectionRule()
                                        .setWorkflowId(workflowId)
                                        .setRuleId(ruleId)
                                        .setRuleType(rule.getType())
                                        .setRule(rule.getRule()))
                .execute() != null;
        log.info("Workflow rule create status for {}/{}: {}", workflowId, ruleId, updated);
        return read(workflowId);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type",
                    value = StoredWorkflowSelectionRule.WF_SELECTION_RULE_TABLE_NAME))
    public Optional<Workflow> deleteSelectionRule(
            @Throws.RuntimeParam("id") String workflowId,
            @Throws.RuntimeParam("subId") String ruleId) {
        val updated = wfDao.lockAndGetExecutor(workflowId)
                .update(wfselDao,
                        createCriteria(StoredWorkflowSelectionRule.class, workflowId)
                                .add(Property.forName(StoredWorkflowSelectionRule.Fields.ruleId).eq(ruleId)),
                        state -> state.setDeleted(true),
                        () -> false)
                .execute();
        log.info("Workflow rule delete status for {}/{}: {}", workflowId, ruleId, updated);
        return read(workflowId);
    }

    private static Workflow toWirePartial(StoredWorkflow wf) {
        return new Workflow(wf.getWorkflowId(),
                            wf.getDisplayName(),
                            wf.getDescription(),
                            wf.getSchemaId(),
                            wf.getTitleTemplate(),
                            wf.getDescriptionTemplate(),
                            wf.getSubjectIdTemplate(),
                            Map.of(),
                            Map.of(),
                            wf.getAvailableActions(),
                            wf.getStartStateId(),
                            Map.of(),
                            wf.getState(),
                            wf.getCreated(),
                            wf.getUpdated());
    }

    private static DetachedCriteria createCriteria(Class<?> clazz, String workflowId) {
        return createCriteria(clazz, workflowId, true);
    }

    private static DetachedCriteria createCriteria(Class<?> clazz, String workflowId, boolean skipDeleted) {
        val criteria = DetachedCriteria.forClass(clazz)
                .add(Property.forName(StoredWorkflow.Fields.workflowId).eq(workflowId))
                .addOrder(Order.asc(StoredWorkflow.Fields.created));
        if (skipDeleted) {
            criteria.add(Property.forName(StoredWorkflow.Fields.deleted).eq(false));
        }
        return criteria;
    }

    private static TicketState toWire(final StoredTicketState state) {
        return new TicketState(
                state.getStateId(),
                state.getDisplayName(),
                state.getDescription(),
                state.isTerminal(),
                state.getAllowedActions(),
                state.getEditableFields(),
                state.getVisibleFields(),
                state.getRequiredFields(),
                state.getVisibleActions(),
                state.getCreated(),
                state.getUpdated());
    }

    private static TicketStateTransition toWire(final StoredTicketStateTransition transition) {
        return new TicketStateTransition(
                transition.getTransitionId(),
                transition.getFromState(),
                transition.getToState(),
                transition.getType(),
                transition.getRule(),
                transition.getActionIds(),
                transition.getWorkflowId(),
                transition.getCreated(),
                transition.getUpdated());
    }
}
