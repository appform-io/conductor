package io.appform.conductor.user.usermanagement;

import io.appform.conductor.core.interfaces.UserStore;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.core.eventmanagement.EventBus;
import io.appform.conductor.model.events.impl.user.UserCreatedEvent;
import io.appform.conductor.model.events.impl.user.UserStateChangeEvent;
import io.appform.conductor.model.events.impl.user.UserUpdatedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingUserStore implements UserStore {
    private final EventBus eventBus;
    private final UserStore userStore;

    @Inject
    public EventGeneratingUserStore(EventBus eventBus, @Named(Constants.CACHED_IMPLEMENTATION_NAME) UserStore userStore) {
        this.eventBus = eventBus;
        this.userStore = userStore;
    }

    @Override
    public Optional<UserSummary> create(String userId, String name, UserType type, String email) {
        val res = userStore.create(userId, name, type, email);
        res.ifPresent(userSummary -> eventBus.publish(new UserCreatedEvent(userSummary.getId())));
        return res;
    }

    @Override
    public Optional<UserSummary> getById(String userId) {
        return userStore.getById(userId);
    }

    @Override
    public List<UserSummary> getByIds(List<String> userIds) {
        return userStore.getByIds(userIds);
    }

    @Override
    public Optional<UserSummary> getByEmail(String email) {
        return userStore.getByEmail(email);
    }

    @Override
    public Optional<UserSummary> update(String userId, UnaryOperator<UserSummary> handler) {
        val res = userStore.update(userId, handler);
        res.ifPresent(userSummary -> eventBus.publish(new UserUpdatedEvent(userSummary.getId())));
        return res;
    }

    @Override
    public Optional<UserSummary> updateState(String userId, UserState state) {
        val res = userStore.updateState(userId, state);
        res.ifPresent(userSummary -> eventBus.publish(new UserStateChangeEvent(userSummary.getId(), userSummary.getState())));
        return res;
    }
}
