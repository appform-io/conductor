package io.appform.conductor.model.events.impl.schema;

import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.EventVisitor;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventSubType(EventType.SCHEMA_DESCRIPTION_UPDATED)
@SuperBuilder
@Jacksonized
public class SchemaDescriptionUpdatedEvent extends Event {
    String description;

    public SchemaDescriptionUpdatedEvent(String schemaId, String description) {
        super(EventType.SCHEMA_DESCRIPTION_UPDATED, ReferredObjectType.SCHEMA, schemaId);
        this.description = description;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}