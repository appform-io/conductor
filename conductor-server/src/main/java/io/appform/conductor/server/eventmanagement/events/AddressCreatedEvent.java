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
public class AddressCreatedEvent extends Event {
    String globalSubjectId;
    String addressId;

    @Builder
    public AddressCreatedEvent(String globalSubjectId,
                               String addressId) {
        super(EventType.ADDRESS_CREATED);
        this.globalSubjectId = globalSubjectId;
        this.addressId = addressId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
