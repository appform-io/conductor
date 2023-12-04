package io.appform.conductor.server.ticketmanagement;

import io.appform.conductor.model.ticket.TicketRelationship;
import lombok.Value;

@Value
public class RelatedTicket {

    String relatedToTicketId;

    TicketRelationship relationship;
}
