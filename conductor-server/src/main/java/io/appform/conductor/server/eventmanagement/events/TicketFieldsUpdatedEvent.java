package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;



@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketFieldsUpdatedEvent extends Event {
    String ticketId;
    List<String> fieldIds;

    @Builder
    public TicketFieldsUpdatedEvent(String ticketId, List<String> fieldIds) {
        super(EventType.TICKET_FIELDS_UPDATED);
        this.ticketId = ticketId;
        this.fieldIds = fieldIds;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}