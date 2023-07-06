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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.appform.jsonrules.Expression;
import lombok.SneakyThrows;

import java.util.Objects;

/**
 *
 */
public class JsonRuleEvaluator implements RuleEvaluator {
    private final ObjectMapper mapper;
    private final LoadingCache<String, Expression> ruleCache;

    public JsonRuleEvaluator(ObjectMapper mapper) {
        this.mapper = mapper;
        this.ruleCache = Caffeine.newBuilder()
                .maximumSize(100_000)
                .build(ruleString -> mapper.readValue(ruleString, Expression.class));
    }

    @Override
    @SneakyThrows
    public boolean evaluate(String rule, JsonNode data) {
        return Objects.requireNonNull(ruleCache.get(rule)).evaluate(data);
    }
}
