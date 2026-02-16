package io.appform.conductor.core.ticketmanagement;

import io.appform.conductor.model.ticket.TicketRelationship;
import lombok.Value;

@Value
public class RelatedTicket {

    String relatedToTicketId;

    TicketRelationship relationship;
}
