package io.appform.conductor.server.ticketmanagement.statemachine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class TriggerData {

    private JsonNode payload;

}
