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

package io.appform.conductor.model.events.analytics;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.events.analytics.impl.EventGroupResponse;
import io.appform.conductor.model.events.analytics.impl.EventListResponse;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "opCode")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "LIST", value = EventListResponse.class),
        @JsonSubTypes.Type(name = "GROUP_BY", value = EventGroupResponse.class),
})
@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class EventQueryResponse {
    private final EventQueryOpCode opCode;

    public abstract <T> T accept(final EventQueryResponseVisitor<T> visitor);
}
