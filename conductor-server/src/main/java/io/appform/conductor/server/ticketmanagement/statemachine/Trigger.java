package io.appform.conductor.server.ticketmanagement.statemachine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class Trigger {

    public enum TriggerSource {
        INGRESS_RAW,
        INGRESS_CALLBACK,
        TICKET_UPDATE,
        TICKET_CREATE,
    }

    private TriggerSource source;
    private JsonNode payload;

}
