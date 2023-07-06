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

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.model.workflow.WorkflowState;
import io.appform.conductor.server.ruleengines.RuleEngine;
import io.appform.conductor.server.utils.Pair;
import io.appform.signals.signals.ScheduledSignal;
import io.dropwizard.lifecycle.Managed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * Selects a workflow from a payload based onselection rules
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class WorkflowSelector implements Managed {
    private static final String HANDLER_NAME = "RELOAD_HANDLER";

    private final Map<Rule, Workflow> workflows = new TreeMap<>(Comparator.comparing(Rule::getType)
                                                                        .thenComparing(Rule::getRule));

    private final ScheduledSignal reloadSignal = new ScheduledSignal(Duration.ofSeconds(30));
    private final StampedLock lock = new StampedLock();
    private final WorkflowStore workflowStore;

    private final RuleEngine ruleEngine;

    @Override
    public void start() throws Exception {
        reloadSignal.connect(HANDLER_NAME, this::reloadWorkflows);
        reloadWorkflows(new Date());
    }

    @Override
    public void stop() throws Exception {
        reloadSignal.disconnect(HANDLER_NAME);
        reloadSignal.close();
    }

    public Optional<Workflow> findWorkflow(final JsonNode payload) {
        val stamp = lock.readLock();
        try {
            return workflows.entrySet()
                    .stream()
                    .filter(entry -> ruleEngine.evaluate(entry.getKey(), payload))
                    .map(Map.Entry::getValue)
                    .findAny();
        }
        finally {
            lock.unlock(stamp);
        }
    }

    private void reloadWorkflows(final Date currTime) {
        log.debug("Refresh called at {}", currTime);
        val stamp = lock.writeLock();
        try {
            val ruleWfMap = workflowStore.list(WorkflowState.ACTIVE)
                    .stream()
                    .flatMap(workflow -> workflow.getSelectionRules().values().stream().map(rule -> new Pair<>(rule, workflow)))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
            workflows.clear();
            workflows.putAll(ruleWfMap);
            log.info("Loaded {} rule workflow pairs", ruleWfMap.size());
        }
        finally {
            lock.unlock(stamp);
        }
    }
}
