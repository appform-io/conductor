/*
 * Copyright (c) 2023 santanu
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

package io.appform.conductor.model.events.analytics.impl;

import io.appform.conductor.model.events.analytics.EventFilters;
import io.appform.conductor.model.events.analytics.EventQueryOpCode;
import io.appform.conductor.model.events.analytics.EventQueryRequest;
import io.appform.conductor.model.events.analytics.EventQueryVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * List events
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EventListRequest extends EventQueryRequest {

    String nextPointer;
    int size;

    @Builder
    @Jacksonized
    public EventListRequest(String requestId, EventFilters filters, String nextPointer, int size) {
        super(EventQueryOpCode.LIST, requestId, filters);
        this.nextPointer = nextPointer;
        this.size = size;
    }

    @Override
    public <T> T accpet(EventQueryVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
