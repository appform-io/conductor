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

import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.analytics.EventQueryOpCode;
import io.appform.conductor.model.events.analytics.EventQueryResponse;
import io.appform.conductor.model.events.analytics.EventQueryResponseVisitor;
import lombok.*;

import java.util.List;

/**
 * Response to event list request
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EventListResponse extends EventQueryResponse {
    List<Event> results;
    String next;

    public EventListResponse(List<Event> results, String next) {
        super(EventQueryOpCode.LIST);
        this.results = results;
        this.next = next;
    }

    @Override
    public <T> T accept(EventQueryResponseVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
