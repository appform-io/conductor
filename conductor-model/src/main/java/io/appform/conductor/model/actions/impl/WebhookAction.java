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
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.ActionVisitor;
import io.appform.conductor.model.workflow.Template;
import lombok.*;

import java.util.Set;

/**
 * Call an external or internal endpoint when this happens. The actual url
 * and payload for the body are constructed using templates.
 */
@Getter
@Setter
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
    String mimeType;

    /**
     * A {@link io.appform.conductor.model.workflow.Template} to generate the headers that need to be passed for the HTTP call.
     * Note: This can contain auth headers etc.
     */
    Template headersTemplate;

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

    public WebhookAction() {
        super(ActionType.WEBHOOK);
    }

    @Override
    public <T> T accept(ActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
