package io.appform.conductor.model.events.impl.user;

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
@EventSubType(EventType.USER_PASSWORD_FAILED_ATTEMPT_UPDATED)
@SuperBuilder
@Jacksonized
public class UserPasswordFailedAttemptUpdatedEvent extends Event {

    int failedAttempts;

    public UserPasswordFailedAttemptUpdatedEvent(String userId, int failedAttempts) {
        super(EventType.USER_PASSWORD_FAILED_ATTEMPT_UPDATED, ReferredObjectType.USER, userId);
        this.failedAttempts = failedAttempts;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}

