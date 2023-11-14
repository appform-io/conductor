package io.appform.conductor.server.ticketmanagement;

import lombok.Value;

@Value
public class RelatedTicket {

    private String relatedTo;

    private TicketRelationship relationship;
}
