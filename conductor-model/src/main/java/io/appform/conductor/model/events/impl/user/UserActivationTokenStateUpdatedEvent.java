package io.appform.conductor.model.events.impl.user;

import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.EventVisitor;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.usermgmt.UserActivationTokenState;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventSubType(EventType.USER_ACTIVATION_TOKEN_STATE_UPDATED)
@SuperBuilder
@Jacksonized
public class UserActivationTokenStateUpdatedEvent extends Event {

    UserActivationTokenState state;

    public UserActivationTokenStateUpdatedEvent(String userId, UserActivationTokenState state) {
        super(EventType.USER_ACTIVATION_TOKEN_STATE_UPDATED, ReferredObjectType.USER, userId);
        this.state = state;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}