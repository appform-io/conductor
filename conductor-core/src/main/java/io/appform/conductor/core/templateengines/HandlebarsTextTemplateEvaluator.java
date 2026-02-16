package io.appform.conductor.core.templateengines;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.core.utils.HandlebarsUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HandlebarsTextTemplateEvaluator implements TextTemplateEvaluator {

    private final ObjectMapper mapper;

    @Override
    @SneakyThrows
    public Optional<String> evaluate(Template template, JsonNode payload) {
        return Optional.of(HandlebarsUtils.handlebars().compileInline(template.getTemplate())
                .apply(mapper.convertValue(payload, new TypeReference<Map<String, Object>>() {})));
    }
}
