package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.*;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketGroupAssignedEvent extends Event {
    String ticketId;
    String groupId;

    @Builder
    public TicketGroupAssignedEvent(String ticketId, String groupId) {
        super(EventType.TICKET_GROUP_ASSIGNED);
        this.ticketId = ticketId;
        this.groupId = groupId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}