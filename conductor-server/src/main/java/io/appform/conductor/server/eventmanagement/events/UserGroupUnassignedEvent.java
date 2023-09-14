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
public class UserGroupUnassignedEvent extends Event {
    String groupId;
    String userId;

    public UserGroupUnassignedEvent(String groupId, String userId) {
        super(EventType.USER_GROUP_UNASSIGNED);
        this.groupId = groupId;
        this.userId = userId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
