package io.appform.conductor.server.actionmanagement.executors;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.appform.conductor.core.actionmanagement.executors.WebhookActionExecutor;
import io.appform.conductor.core.templateengines.*;
import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.WebhookAction;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.fields.StringFieldSchema;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.TicketSummary;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.fields.impl.StringFieldValue;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.core.actionmanagement.ActionExecutor;
import io.appform.conductor.server.templateengines.*;
import io.appform.conductor.core.utils.ConductorServerUtils;
import io.dropwizard.jackson.Jackson;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
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
                new HandlebarsTextTemplateEvaluator(mapper),
                new FixedObjectTemplateEvaluator(mapper));
        val httpClient = ConductorServerUtils.createHttpClient();
        //create webhookActionExecutor
        WebhookActionExecutor webhookActionExecutor = new WebhookActionExecutor(templateEngine, httpClient, mapper);

        //create a actionEvalData
        val payload = mapper.valueToTree(Map.of("phoneNumber", "1234567890", "name", "John", "age", 25, "userId", "userId"));
        val actionEvalData = new ActionExecutor.ActionEvalData(null, schema(), ticket(), payload, null);

        //create a webhook action
        WebhookAction webhookAction = WebhookAction.builder()
                .callType(WebhookAction.CallType.POST)
                .urlTemplate(new Template(Template.Type.HANDLEBARS, "http://localhost:" + wm.getHttpPort() + "/update/subject/{{payload.phoneNumber}}/details"))
                .payloadTemplate(new Template(Template.Type.HANDLEBARS, "{\"name\":\"${payload.name}\",\"age\":{{payload.age}}"))
                .headerTemplates(Map.of("AuthorizationForId", new Template(Template.Type.HANDLEBARS, "{{payload.userId}}")))
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
                new HandlebarsTextTemplateEvaluator(mapper),
                new FixedObjectTemplateEvaluator(mapper));
        val httpClient = ConductorServerUtils.createHttpClient();
        //create webhookActionExecutor
        WebhookActionExecutor webhookActionExecutor = new WebhookActionExecutor(templateEngine, httpClient, mapper);

        //create a actionEvalData
        val payload = mapper.valueToTree(Map.of("phoneNumber", "1234567890", "name", "John", "age", 25, "userId", "userId"));
        val actionEvalData = new ActionExecutor.ActionEvalData(null, schema(), ticket(), payload, null);

        //create a webhook action
        WebhookAction webhookAction = WebhookAction.builder()
                .callType(WebhookAction.CallType.POST)
                .urlTemplate(new Template(Template.Type.HANDLEBARS, "http://localhost:" + wm.getHttpPort() + "/update2/subject/{{payloadphoneNumber}}/details"))
                .payloadTemplate(new Template(Template.Type.HANDLEBARS, "{\"name\":\"{{payload.name}}\",\"age\":{{age}}"))
                .headerTemplates(Map.of("AuthorizationForId", new Template(Template.Type.HANDLEBARS, "{{userId}}")))
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


    private TicketDetails ticket() {
        return new TicketDetails(new TicketSummary("ticketId",
                "title",
                "description",
                "workflowId",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new Date(),
                new Date()),
                List.of(new TicketField(FieldType.STRING,
                        ConductorServerUtils.readableId("TS1", "firstName"),
                        new StringFieldValue("Tushar"),
                        new Date(),
                        new Date())),
                List.of());
    }

    private Schema schema() {
       return new Schema("TS1",
                "Test Schema",
                "Test",
                1,
                SchemaState.ACTIVE,
                null,
               null,
                List.of(
                        new StringFieldSchema(ConductorServerUtils.readableId("TS1","firstName"),
                                "firstName",
                                "First Name",
                                "",
                                null,
                                null,
                                null,
                                false,
                                new Date(),
                                new Date(),
                                200,
                                null,
                                null),
                        new StringFieldSchema(ConductorServerUtils.readableId("TS1","lastName"),
                                "lastName",
                                "Last Name",
                                "",
                                null,
                                null,
                                null,
                                false,
                                new Date(),
                                new Date(),
                                200,
                                null,
                                null)
                ),
                new Date(),
                new Date());
    }
}
