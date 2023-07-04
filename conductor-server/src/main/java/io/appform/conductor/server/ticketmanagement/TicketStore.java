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

package io.appform.conductor.server.ticketmanagement;

import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.filter.QueryTimeWindow;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A place to store {@link io.appform.conductor.model.ticket.TicketSummary}
 */
public interface TicketStore {

    Optional<TicketSkeleton> create(
            String ticketId,
            String title,
            String description,
            String workflowId,
            String subjectId,
            String ticketStateId,
            TicketPriority priority,
            List<TicketFieldData> fields);

    Optional<TicketSkeleton> read(String ticketId, boolean readFields);

    Optional<TicketSkeleton> update(
            String ticketId,
            String title,
            String description,
            String subjectId,
            String ticketStateId,
            TicketPriority priority,
            List<TicketFieldData> fields);

    List<TicketSkeleton> list(
            QueryTimeWindow timeWindow, List<TicketFieldFilter> filters, int start, int size,
            Map<String, FieldSchema> relevantFieldSchema);

}
