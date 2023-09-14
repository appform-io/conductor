package io.appform.conductor.server.eventmanagement.events;

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
public class SchemaFieldAddedEvent extends Event {
    String schemaId;
    String fieldId;

    @Builder
    public SchemaFieldAddedEvent(String schemaId, String fieldId) {
        super(EventType.SCHEMA_FIELD_ADDED);
        this.schemaId = schemaId;
        this.fieldId = fieldId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}