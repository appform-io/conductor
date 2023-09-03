/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.model.actions.impl;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.ActionScope;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.ActionVisitor;
import io.appform.conductor.model.workflow.Template;
import lombok.*;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Call an external or internal endpoint when this happens. The actual url
 * and payload for the body are constructed using templates.
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WebhookAction extends Action {

    /**
     * HTTP verb for the call... is it a GET/POST etc
     */
    public enum CallType {
        GET,
        PUT,
        POST,
        PATCH,
        DELETE
    }

    /**
     * Supported MIME types for the call. Payload template will be interpreted based on that
     */
    @Getter
    public enum MimeType {
        JSON("application/json"),
        FORM("application/x-www-form-urlencoded"),
        TEXT("text/plain"),
        XML("application/xml")
        ;

        private final String contentType;

        MimeType(String contentType) {
            this.contentType = contentType;
        }
    }

    /**
     * Is the call sync or async
     */
    public enum CallMode {
        SYNC,
        ASYNC
    }

    /**
     * How the call will get retried in case of failures
     */
    public enum RetryStrategy {
        NO_RETRY,
        FIXED_INTERVAL,
        EXPONENTIAL_BACKOFF
    }

    /**
     * A {@link io.appform.conductor.model.workflow.Template} to generate URL for template.
     * Note that URL will be URL encoded post template evaluation
     */
    Template urlTemplate;

    /**
     * Type for call as detailed in {@link CallType}
     */
    CallType callType;

    /**
     * A {@link io.appform.conductor.model.workflow.Template} to generate the body of the HTTP call.
     * Note that body will only be passed for HTTP methods that allow for it.
     */
    Template payloadTemplate;

    /**
     * The mimetype of the body
     */
    MimeType mimeType;

    /**
     * A {@link io.appform.conductor.model.workflow.Template} to generate the header value that need to be passed for the HTTP call.
     * Note: This can contain auth headers etc.
     */
    Map<String, Template> headerTemplates;

    /**
     * The HTTP response codes that will be considered as success. Once successful, no retry will kick in.
     */
    Set<Integer> successCodes;

    /**
     * How the call will be made, as detailed in {@link CallMode}
     */
    CallMode callMode;

    /**
     * Timeout for the HTTP call in milliseconds
     */
    int timeoutMs;

    /**
     * Retry strategy for the call, in case the call times out or returns a HTTP code not in {@link #successCodes}
     */
    RetryStrategy retryStrategy;

    /**
     * In case a retyr is configured, what is the maximum number of retries to be performed
     */
    int numRetries;

    @Builder
    public WebhookAction(
            String id,
            String name,
            String description,
            ActionScope scope,
            Date created,
            Date updated,
            Template urlTemplate,
            CallType callType,
            Template payloadTemplate,
            MimeType mimeType,
            Map<String,Template> headerTemplates,
            Set<Integer> successCodes, CallMode callMode,
            int timeoutMs,
            RetryStrategy retryStrategy,
            int numRetries) {
        super(ActionType.WEBHOOK, id, name, description, scope, created, updated);
        this.urlTemplate = urlTemplate;
        this.callType = callType;
        this.payloadTemplate = payloadTemplate;
        this.mimeType = mimeType;
        this.headerTemplates = headerTemplates;
        this.successCodes = successCodes;
        this.callMode = callMode;
        this.timeoutMs = timeoutMs;
        this.retryStrategy = retryStrategy;
        this.numRetries = numRetries;
    }

    @Override
    public <T> T accept(ActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
