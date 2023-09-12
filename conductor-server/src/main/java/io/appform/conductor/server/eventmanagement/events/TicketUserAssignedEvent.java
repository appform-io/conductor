package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.*;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketUserAssignedEvent extends Event {
    String ticketId;
    String userId;

    @Builder
    public TicketUserAssignedEvent(String ticketId, String userId) {
        super(EventType.TICKET_USER_ASSIGNED);
        this.ticketId = ticketId;
        this.userId = userId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}