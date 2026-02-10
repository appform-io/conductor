package io.appform.conductor.server.templateengines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.workflow.Template;
import io.dropwizard.jackson.Jackson;
import org.junit.jupiter.api.Test;

public class StringSubstitutionTextTemplateEvaluatorTest {

    private ObjectMapper mapper = Jackson.newObjectMapper();

    private StringSubstitutionTextTemplateEvaluator evaluator = new StringSubstitutionTextTemplateEvaluator(mapper);

    @Test
    public void testEvaluate() throws JsonProcessingException {
        String templateStr = "Hello ${name}, your age is ${age}";
        Template template = new Template(Template.Type.STRING_SUBSTITUTION, templateStr);
        JsonNode payload =  mapper.readTree("""
                {
                    "name": "Alice",
                    "age": 30,
                    "place": {
                        "name": "Wonderland",
                        "city": "Fictional City"
                     }
                }
                """);
        String result = evaluator.evaluate(template, payload).orElse(null);
        assert result != null;
        assert result.equals("Hello Alice, your age is 30");
    }

    @Test
    public void testEvaluateWithMissingVariable() throws JsonProcessingException {
        String templateStr = "Hello ${name}, your age is ${age}, you live in ${place.name}";
        Template template = new Template(Template.Type.STRING_SUBSTITUTION, templateStr);
        JsonNode payload =  mapper.readTree("""
                {
                    "name": "Alice",
                    "age": 30
                }
                """);
        String result = evaluator.evaluate(template, payload).orElse(null);
        assert result != null;
        assert result.equals("Hello Alice, your age is 30, you live in ${place.name}");
    }

    @Test
    public void testEvaluateWithNestedVariableIgnored() throws JsonProcessingException {
        String templateStr = "Hello ${name}, your age is ${age}, you live in ${place.name}";
        Template template = new Template(Template.Type.STRING_SUBSTITUTION, templateStr);
        JsonNode payload =  mapper.readTree("""
                {
                    "name": "Alice",
                    "age": 30,
                    "place": {
                        "name": "Wonderland"
                     }
                }
                """);
        String result = evaluator.evaluate(template, payload).orElse(null);
        assert result != null;
        assert result.equals("Hello Alice, your age is 30, you live in ${place.name}");
    }
}
