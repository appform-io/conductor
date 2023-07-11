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

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluate a template from the payload and return a string
 */
@Singleton
public class TemplateEngine {
    private final Map<Template.Type, TextTemplateEvaluator> textEvaluators;
    private final Map<Template.Type, ObjectTemplateEvaluator> objectEvaluators;

    @Inject
    public TemplateEngine(
            FixedTextTemplateEvaluator fixedTextTemplateEvaluator,
            StringSubstitutionTextTemplateEvaluator stringSubstitutionTemplateEvaluator,
            FixedObjectTemplateEvaluator fixedObjectTemplateEvaluator) {
        textEvaluators = Map.of(Template.Type.FIXED, fixedTextTemplateEvaluator,
                                  Template.Type.STRING_SUBSTITUTION, stringSubstitutionTemplateEvaluator);
        objectEvaluators = Map.of(Template.Type.FIXED, fixedObjectTemplateEvaluator);
    }


    public Optional<String> evaluateToText(Template template, JsonNode payload) {
        val engine = textEvaluators.get(template.getType());
        ConductorServerUtils.ensureCondition(null != engine,
                                             ConductorErrorCode.INVALID_TEMPLATE_TYPE,
                                             Map.of("type", template.getType()));
        assert engine != null;
        return engine.evaluate(template, payload);
    }

    public <T> Optional<T> evaluateToObject(Template template, JsonNode payload, Class<T> clazz) {
        val engine = objectEvaluators.get(template.getType());
        ConductorServerUtils.ensureCondition(null != engine,
                                             ConductorErrorCode.INVALID_TEMPLATE_TYPE,
                                             Map.of("type", template.getType()));
        assert engine != null;
        return engine.evaluate(template, payload, clazz);
    }

}
