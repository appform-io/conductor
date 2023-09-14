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
public class UserRoleRevokedEvent extends Event {
    String userId;
    String roleId;

    @Builder
    public UserRoleRevokedEvent(String userId, String roleId) {
        super(EventType.USER_ROLE_REVOKED);
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}