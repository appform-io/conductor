package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.model.schema.SchemaState;
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
public class SchemaStateUpdatedEvent extends Event {
    String id;
    SchemaState state;

    @Builder
    public SchemaStateUpdatedEvent(String id, SchemaState state) {
        super(EventType.SCHEMA_STATE_UPDATED);
        this.id = id;
        this.state = state;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}