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
public class AddressUpdatedEvent extends Event {
    String globalSubjectId;
    String addressId;

    @Builder
    public AddressUpdatedEvent(String globalSubjectId, String id) {
        super(EventType.ADDRESS_UPDATED);
        this.globalSubjectId = globalSubjectId;
        this.addressId = id;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
