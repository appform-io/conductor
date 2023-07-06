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

package io.appform.conductor.server.ruleengines;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.conductor.model.workflow.Rule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Top level rule engine to be used everywhere
 */
@Singleton
public class RuleEngine {
    private final Map<Rule.RuleType, RuleEvaluator> engines;

    @Inject
    public RuleEngine(final HopeRuleEvaluator hopeRuleEvaluator,
                      final JsonRuleEvaluator jsonRuleEvaluator) {
        this.engines = Map.of(Rule.RuleType.HOPE, hopeRuleEvaluator,
                              Rule.RuleType.JSON_RULE, jsonRuleEvaluator);
    }

    public boolean evaluate(final Rule rule, final JsonNode payload) {
        return engines.get(rule.getType())
                .evaluate(rule.getRule(), payload);
    }
}
