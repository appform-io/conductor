package io.appform.conductor.server.actionmanagement.executors;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.WebhookAction;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.templateengines.FixedObjectTemplateEvaluator;
import io.appform.conductor.server.templateengines.FixedTextTemplateEvaluator;
import io.appform.conductor.server.templateengines.StringSubstitutionTextTemplateEvaluator;
import io.appform.conductor.server.templateengines.TemplateEngine;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.dropwizard.jackson.Jackson;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@WireMockTest
public class WebhookActionExecutorTest {

    @Test
    void testWebhookActionExecutorSuccess(final WireMockRuntimeInfo wm) {
        val mapper = Jackson.newObjectMapper();
        val templateEngine = new TemplateEngine(new FixedTextTemplateEvaluator(),
                new StringSubstitutionTextTemplateEvaluator(mapper),
                new FixedObjectTemplateEvaluator(mapper));
        val httpClient = ConductorServerUtils.createHttpClient();
        //create webhookActionExecutor
        WebhookActionExecutor webhookActionExecutor = new WebhookActionExecutor(templateEngine, httpClient);

        //create a actionEvalData
        val ticketJsonNode = mapper.valueToTree(Map.of("phoneNumber", "1234567890", "name", "John", "age", 25, "userId", "userId"));
        val actionEvalData = new ActionExecutor.ActionEvalData(null,null,null,
                ticketJsonNode, null);

        //create a webhook action
        WebhookAction webhookAction = WebhookAction.builder()
                .callType(WebhookAction.CallType.POST)
                .urlTemplate(new Template(Template.Type.STRING_SUBSTITUTION, "http://localhost:" + wm.getHttpPort() + "/update/subject/${phoneNumber}/details"))
                .payloadTemplate(new Template(Template.Type.STRING_SUBSTITUTION, "{\"name\":\"${name}\",\"age\":${age}}"))
                .headerTemplates(Map.of("AuthorizationForId", new Template(Template.Type.STRING_SUBSTITUTION, "${userId}")))
                .successCodes(Set.of(200,202))
                .timeoutMs(1000)
                .retryStrategy(WebhookAction.RetryStrategy.NO_RETRY)
                .build();


        //stub the response
        stubFor(post("/update/subject/1234567890/details")
                .withHeader("AuthorizationForId", new EqualToPattern("userId"))
                .willReturn(okForJson("{\"status\":\"ACCEPTED\"}")));

        //call execute method
        val actionExecutionResult =  webhookActionExecutor.run(webhookAction, actionEvalData);

        //verify the response
        assertEquals(ActionExecutionResult.SUCCESS, actionExecutionResult);
    }

    @Test
    void testWebhookActionExecutorFail(final WireMockRuntimeInfo wm) {
        val mapper = Jackson.newObjectMapper();
        val templateEngine = new TemplateEngine(new FixedTextTemplateEvaluator(),
                new StringSubstitutionTextTemplateEvaluator(mapper),
                new FixedObjectTemplateEvaluator(mapper));
        val httpClient = ConductorServerUtils.createHttpClient();
        //create webhookActionExecutor
        WebhookActionExecutor webhookActionExecutor = new WebhookActionExecutor(templateEngine, httpClient);

        //create a actionEvalData
        val ticketJsonNode = mapper.valueToTree(Map.of("phoneNumber", "1234567890", "name", "John", "age", 25, "userId", "userId"));
        val actionEvalData = new ActionExecutor.ActionEvalData(null,null,null,
                ticketJsonNode, null);

        //create a webhook action
        WebhookAction webhookAction = WebhookAction.builder()
                .callType(WebhookAction.CallType.POST)
                .urlTemplate(new Template(Template.Type.STRING_SUBSTITUTION, "http://localhost:" + wm.getHttpPort() + "/update2/subject/${phoneNumber}/details"))
                .payloadTemplate(new Template(Template.Type.STRING_SUBSTITUTION, "{\"name\":\"${name}\",\"age\":${age}}"))
                .headerTemplates(Map.of("AuthorizationForId", new Template(Template.Type.STRING_SUBSTITUTION, "${userId}")))
                .successCodes(Set.of(200,202))
                .timeoutMs(1000)
                .retryStrategy(WebhookAction.RetryStrategy.NO_RETRY)
                .build();


        //stub the response
        stubFor(post("/update2/subject/1234567890/details")
                .withHeader("AuthorizationForId", new EqualToPattern("userId"))
                .willReturn(notFound()));

        //call execute method
        val actionExecutionResult =  webhookActionExecutor.run(webhookAction, actionEvalData);

        //verify the response
        assertEquals(ActionExecutionResult.FAILURE, actionExecutionResult);
    }
}
