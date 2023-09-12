package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserActivationTokenGeneratedEvent extends Event {
    String userId;

    public UserActivationTokenGeneratedEvent(String userId) {
        super(EventType.USER_ACTIVATION_TOKEN_GENERATED);
        this.userId = userId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
