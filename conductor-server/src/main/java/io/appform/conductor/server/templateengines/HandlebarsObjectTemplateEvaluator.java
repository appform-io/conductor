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

package io.appform.conductor.server.templateengines;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.utils.HandlebarsUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HandlebarsObjectTemplateEvaluator implements ObjectTemplateEvaluator {
    private final ObjectMapper mapper;

    @Override
    @SneakyThrows
    public <T> Optional<T> evaluate(Template template, JsonNode payload, Class<T> clazz) {
        return Optional.of(HandlebarsUtils.handlebars().compileInline(template.getTemplate())
                        .apply(mapper.convertValue(payload, new TypeReference<Map<String, Object>>() {
                        })))
                .map(s -> {
                    try {
                        return mapper.readValue(s, clazz);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse substituted string to object", e);
                    }
                });
    }
}
