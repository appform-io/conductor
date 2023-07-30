package io.appform.conductor.server.actionmanagement.executors;


import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.WebhookAction;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.templateengines.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static io.appform.conductor.model.actions.impl.WebhookAction.CallType.GET;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class WebhookActionExecutor {

    private final TemplateEngine templateEngine;
    private final CloseableHttpClient httpClient;

    public ActionExecutionResult run(WebhookAction webhookAction, ActionExecutor.ActionEvalData evalData) {
        val request = buildRequest(webhookAction, evalData);
        val retryPolicy = retryPolicy(webhookAction.getRetryStrategy());
        return Failsafe.with(retryPolicy).onFailure(result -> {
            val failure = result.getFailure();
            if (null != failure) {
                log.error("WebhookAction failed with error: {}", failure.getMessage());
            } else {
                log.error("WebhookAction failed with response: {}", result.getResult());
            }
        }).get(() -> call(request, webhookAction));
    }

    private ActionExecutionResult call(HttpUriRequestBase request, WebhookAction webhookAction) {
        try (val response = httpClient.execute(request, classicHttpResponse -> classicHttpResponse)) {
            val body = EntityUtils.toString(response.getEntity());
            int code = response.getCode();
            if (webhookAction.getSuccessCodes().contains(code)) {
                return ActionExecutionResult.SUCCESS;
            } else {
                log.info("Received non-successful response code: {}, body:{}", code, body);
                return ActionExecutionResult.FAILURE;
            }
        } catch (IOException | ParseException e) {
            log.error("Error while making webhook action: " + webhookAction.getId(), e);
            return ActionExecutionResult.FAILURE;
        }
    }

    private HttpUriRequestBase buildRequest(WebhookAction webhookAction, ActionExecutor.ActionEvalData evalData) {
        String url = templateEngine.evaluateToText(webhookAction.getUrlTemplate(),
                evalData.getTicketJson()).orElse(null);
        //Creating Http request
        val request = switch (webhookAction.getCallType()) {
            case GET -> new HttpGet(url);
            case PUT -> new HttpPut(url);
            case POST -> new HttpPost(url);
            case PATCH -> new HttpPatch(url);
            case DELETE -> new HttpDelete(url);
        };

        //Adding headers only if value is not null
        webhookAction.getHeaderTemplates()
                .forEach((key, value) -> templateEngine.evaluateToText(value,
                        evalData.getTicketJson()).ifPresent(headerValue -> request.setHeader(key, headerValue)));

        //Adding payload to non GET calls
        if (webhookAction.getCallType() != GET
                && webhookAction.getPayloadTemplate() != null) {
            templateEngine.evaluateToText(webhookAction.getPayloadTemplate(),
                    evalData.getTicketJson()).ifPresent(payload ->
                    request.setEntity(new StringEntity(payload)));
        }

        //Adding request timeout
        request.setConfig(RequestConfig.custom()
                .setResponseTimeout(Timeout.of(webhookAction.getTimeoutMs(), TimeUnit.MILLISECONDS))
                .build());
        return request;
    }

    private RetryPolicy<ActionExecutionResult> retryPolicy(WebhookAction.RetryStrategy retryStrategy) {
        return switch (retryStrategy) {
            case NO_RETRY -> new RetryPolicy<ActionExecutionResult>()
                    .withMaxAttempts(1)
                    .handle(Exception.class)
                    .handleResultIf(response -> !ActionExecutionResult.SUCCESS.equals(response));
            case FIXED_INTERVAL -> new RetryPolicy<ActionExecutionResult>()
                    .withMaxAttempts(3)
                    .withDelay(Duration.ofMillis(500))
                    .withMaxDuration(Duration.ofSeconds(5))
                    .handle(Exception.class)
                    .handleResultIf(response -> !ActionExecutionResult.SUCCESS.equals(response));
            case EXPONENTIAL_BACKOFF -> new RetryPolicy<ActionExecutionResult>()
                    .withMaxAttempts(3)
                    .withBackoff(500, 3000, ChronoUnit.MILLIS, 2)
                    .withMaxDuration(Duration.ofSeconds(5))
                    .handle(Exception.class)
                    .handleResultIf(response -> !ActionExecutionResult.SUCCESS.equals(response));
        };
    }
}
