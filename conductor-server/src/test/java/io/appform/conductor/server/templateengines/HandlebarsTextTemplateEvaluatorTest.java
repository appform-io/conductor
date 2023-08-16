package io.appform.conductor.server.templateengines;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.workflow.Template;
import io.dropwizard.jackson.Jackson;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HandlebarsTextTemplateEvaluatorTest {

    private ObjectMapper mapper = Jackson.newObjectMapper();

    @Test
    public void testHandlebarTextTemplateEvaluatorSuccess() throws Exception {
        val handlebarTextTemplateEvaluator = new HandlebarsTextTemplateEvaluator(mapper);
        val payload = mapper.readTree("""
                {
                  "subject" : {
                     "number" : "12345"
                     }
                }
                """);
        val template = new Template(Template.Type.HANDLEBARS, "{{subject.number}}");
        val extractedSubject = handlebarTextTemplateEvaluator.evaluate(template, payload);
        assertTrue(extractedSubject.isPresent());
        assertEquals("12345", extractedSubject.get());
    }


}
