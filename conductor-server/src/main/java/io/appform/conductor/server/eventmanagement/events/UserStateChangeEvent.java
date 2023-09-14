package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.model.usermgmt.UserState;
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
public class UserStateChangeEvent extends Event {

    String id;
    UserState state;

    @Builder
    public UserStateChangeEvent(String id, UserState state) {
        super(EventType.USER_STATE_CHANGED);
        this.id = id;
        this.state = state;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
