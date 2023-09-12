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
public class ActionCreatedEvent extends Event {
    String id;

    @Builder
    public ActionCreatedEvent(String id) {
        super(EventType.ACTION_CREATED);
        this.id = id;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}