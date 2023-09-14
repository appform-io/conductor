package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;



@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketPriorityUpdatedEvent extends Event {
    String ticketId;
    TicketPriority priority;

    @Builder
    public TicketPriorityUpdatedEvent(String ticketId, TicketPriority priority) {
        super(EventType.TICKET_PRIORITY_UPDATED);
        this.ticketId = ticketId;
        this.priority = priority;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}