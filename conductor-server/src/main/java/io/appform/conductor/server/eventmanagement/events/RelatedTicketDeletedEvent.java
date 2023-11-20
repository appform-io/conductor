package io.appform.conductor.server.eventmanagement.events;

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
public class RelatedTicketDeletedEvent extends Event {
    String ticketId;
    String relatedToTicketId;

    @Builder
    public RelatedTicketDeletedEvent(String ticketId, String relatedToTicketId) {
        super(EventType.RELATED_TICKET_DELETED);
        this.ticketId = ticketId;
        this.relatedToTicketId = relatedToTicketId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}