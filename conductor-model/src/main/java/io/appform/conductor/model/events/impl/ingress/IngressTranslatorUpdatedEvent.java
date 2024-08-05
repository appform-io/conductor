package io.appform.conductor.model.events.impl.ingress;

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
@EventSubType(EventType.INGRESS_TRANSLATOR_UPDATED)
@SuperBuilder
@Jacksonized
public class IngressTranslatorUpdatedEvent extends Event {

    public IngressTranslatorUpdatedEvent(String id) {
        super(EventType.INGRESS_TRANSLATOR_UPDATED, ReferredObjectType.INGRESS_TRANSLATOR, id);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}