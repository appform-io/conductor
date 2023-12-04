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

package io.appform.conductor.model.events.impl.ticket;

import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.EventVisitor;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventSubType(EventType.TICKET_PRIORITY_UPDATED)
@SuperBuilder
@Jacksonized
public class TicketPriorityUpdatedEvent extends Event {
    TicketPriority priority;

    public TicketPriorityUpdatedEvent(String ticketId, TicketPriority priority) {
        super(EventType.TICKET_PRIORITY_UPDATED, ReferredObjectType.TICKET, ticketId);
        this.priority = priority;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}