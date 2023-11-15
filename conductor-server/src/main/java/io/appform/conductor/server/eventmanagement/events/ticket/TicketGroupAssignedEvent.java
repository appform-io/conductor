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

package io.appform.conductor.server.eventmanagement.events.ticket;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import io.appform.conductor.server.eventmanagement.events.ReferredObjectType;
import lombok.*;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketGroupAssignedEvent extends Event {
    String groupId;

    public TicketGroupAssignedEvent(String ticketId, String groupId) {
        super(EventType.TICKET_GROUP_ASSIGNED, ReferredObjectType.TICKET, ticketId);
        this.groupId = groupId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}