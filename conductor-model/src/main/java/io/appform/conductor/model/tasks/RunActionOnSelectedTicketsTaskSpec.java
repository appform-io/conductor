/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.model.tasks;

import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RunActionOnSelectedTicketsTaskSpec extends TaskSpec {
    private List<TicketFilter> ticketFilters;
    private List<TicketFieldFilter> fieldFilters;
    private List<String> actionIds;

    @Builder
    @Jacksonized
    public RunActionOnSelectedTicketsTaskSpec(
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            List<String> actionIds) {
        super(TaskType.RUN_ACTION_ON_SELECTED_TICKETS);
        this.ticketFilters = ticketFilters;
        this.fieldFilters = fieldFilters;
        this.actionIds = actionIds;
    }

    @Override
    public <T> T accept(TaskSpecVisitor<T> task) {
        return task.visit(this);
    }
}
