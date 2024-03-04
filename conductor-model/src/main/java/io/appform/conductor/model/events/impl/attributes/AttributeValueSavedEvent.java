package io.appform.conductor.model.events.impl.attributes;

import io.appform.conductor.model.attributes.AttributeScopeType;
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
@EventSubType(EventType.ATTRIBUTE_VALUES_SAVED)
@SuperBuilder
@Jacksonized
public class AttributeValueSavedEvent extends Event {

    AttributeScopeType scopeType;

    public AttributeValueSavedEvent(AttributeScopeType scopeType,
                                    String objectRefId) {
        super(EventType.ATTRIBUTE_VALUES_SAVED, ReferredObjectType.ATTRIBUTE_VALUE, objectRefId);
        this.scopeType = scopeType;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}