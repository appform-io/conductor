package io.appform.conductor.server.ticketmanagement;

import lombok.Value;

@Value
public class RelatedTicket {

    String relatedToTicketId;

    TicketRelationship relationship;
}
