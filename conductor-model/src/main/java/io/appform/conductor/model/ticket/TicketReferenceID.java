package io.appform.conductor.model.ticket;

import lombok.Value;


@Value
public class TicketReferenceID {

    private String source;

    private String refId;
}
