package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.*;



@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketStateUpdatedEvent extends Event {
    String ticketId;
    String stateId;

    @Builder
    public TicketStateUpdatedEvent(String ticketId, String stateId) {
        super(EventType.TICKET_STATE_UPDATED);
        this.ticketId = ticketId;
        this.stateId = stateId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}