package io.appform.conductor.server.ticketmanagement.statemachine.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class TriggerData {

    private JsonNode payload;

}
