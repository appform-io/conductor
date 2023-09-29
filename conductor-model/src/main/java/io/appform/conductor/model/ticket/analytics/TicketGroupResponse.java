/*
 * Copyright (c) 2023 Santanu Sinha
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

package io.appform.conductor.model.ticket.analytics;

import com.google.common.collect.TreeBasedTable;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketGroupResponse extends TicketQueryResponse {

    @Value
    @With
    public static class GroupResponse {
        String fieldName;
        Map<String, Long> counts = new TreeMap<>();
        Map<String, GroupResponse> children = new TreeMap<>();
    }

    TreeBasedTable<Integer, String, Object> counts;

    @Builder
    @Jacksonized
    public TicketGroupResponse(String requestId, TreeBasedTable<Integer, String, Object> counts) {
        super(TicketQueryOpCode.GROUP, requestId);
        this.counts = counts;
    }

    @Override
    public <T> T accept(TicketQueryResponseVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
