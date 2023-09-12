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
public class UserSessionCreatedEvent extends Event {

    String sessionId;
    String userId;

    public UserSessionCreatedEvent(String sessionId, String userId) {
        super(EventType.USER_SESSION_CREATED);
        this.sessionId = sessionId;
        this.userId = userId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
