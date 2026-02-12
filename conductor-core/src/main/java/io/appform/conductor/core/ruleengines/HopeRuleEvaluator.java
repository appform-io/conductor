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
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;

import javax.inject.Singleton;

/**
 * Evaluates a hope rule and returns true or false
 */
@Singleton
public class HopeRuleEvaluator implements RuleEvaluator {
    private final HopeLangEngine hopeLangEngine = HopeLangEngine.builder()
            .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
            .build();
    private final LoadingCache<String, Evaluatable> ruleCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build(hopeLangEngine::parse);

    @Override
    public boolean evaluate(String rule, JsonNode data) {
        return hopeLangEngine.evaluate(ruleCache.get(rule), data);
    }
}
