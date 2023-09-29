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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketTimeSeriesResponse extends TicketQueryResponse {
    public static final String DEFAULT_FIELD = "_series_";
    /**
     * Map of time series. Each map key is a dataset to be rendered.
     */
    TreeBasedTable<Integer, String, Object> series;

    @Builder
    @Jacksonized
    public TicketTimeSeriesResponse(String requestId, TreeBasedTable<Integer, String, Object> series) {
        super(TicketQueryOpCode.TIME_SERIES, requestId);
        this.series = series;
    }

    @Override
    public <T> T accept(TicketQueryResponseVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
