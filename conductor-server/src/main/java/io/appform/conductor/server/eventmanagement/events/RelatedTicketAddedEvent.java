package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import io.appform.conductor.server.ticketmanagement.TicketRelationship;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RelatedTicketAddedEvent extends Event {
    String ticketId;
    String relatedToTicketId;
    TicketRelationship relationship;

    @Builder
    public RelatedTicketAddedEvent(String ticketId, String relatedToTicketId, TicketRelationship relationship) {
        super(EventType.RELATED_TICKET_ADDED);
        this.ticketId = ticketId;
        this.relatedToTicketId = relatedToTicketId;
        this.relationship = relationship;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}