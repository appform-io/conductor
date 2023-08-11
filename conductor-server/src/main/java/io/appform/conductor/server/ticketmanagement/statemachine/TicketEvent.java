package io.appform.conductor.server.ticketmanagement.statemachine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class TicketEvent {

    public enum EventSource {
        INGRESS_RAW,
        INGRESS_CALLBACK,
        TICKET_UPDATE,
    }

    private EventSource source;
    private JsonNode payload;

}
