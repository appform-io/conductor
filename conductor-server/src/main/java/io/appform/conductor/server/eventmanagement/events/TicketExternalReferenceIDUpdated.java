package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.model.ticket.ExternalReferenceID;
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
public class TicketExternalReferenceIDUpdated extends Event {
    String ticketId;
    ExternalReferenceID referenceID;

    @Builder
    public TicketExternalReferenceIDUpdated(String ticketId, ExternalReferenceID referenceID) {
        super(EventType.TICKET_EXTERNAL_REFERENCE_ID_UPDATED);
        this.ticketId = ticketId;
        this.referenceID = referenceID;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}