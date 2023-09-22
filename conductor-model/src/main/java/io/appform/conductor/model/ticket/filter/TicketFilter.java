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

package io.appform.conductor.model.ticket.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Filter based on ticket core properties and not fields
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "WORKFLOW_EQUALS", value = TicketWorkflowEquals.class),
        @JsonSubTypes.Type(name = "CREATED_BY", value = TicketCreatedBy.class),
        @JsonSubTypes.Type(name = "ASSIGNED_TO_GROUP", value = TicketAssignedToGroup.class),
        @JsonSubTypes.Type(name = "UNASSIGNED_TO_GROUP", value = TicketUnAssignedToGroup.class),
        @JsonSubTypes.Type(name = "ASSIGNED_TO_USER", value = TicketAssignedToUser.class),
        @JsonSubTypes.Type(name = "UNASSIGNED_TO_USER", value = TicketUnAssignedToUser.class),
        @JsonSubTypes.Type(name = "SUBJECT_EQUALS", value = TicketSubjectEquals.class),
        @JsonSubTypes.Type(name = "STATE_EQUALS", value = TicketStateEquals.class),
        @JsonSubTypes.Type(name = "STATE_IN", value = TicketStateIn.class),
        @JsonSubTypes.Type(name = "PRIORITY_IN", value = TicketPriorityIn.class),
        @JsonSubTypes.Type(name = "CREATED_TIME_WINDOW", value = TicketsCreatedTimeWindow.class),
        @JsonSubTypes.Type(name = "UPDATED_TIME_WINDOW", value = TicketsUpdatedTimeWindow.class),
})
public abstract class TicketFilter {
    private final TicketFilterType type;

    public abstract <T> T accept(final TicketFilterVisitor<T> visitor);
}
