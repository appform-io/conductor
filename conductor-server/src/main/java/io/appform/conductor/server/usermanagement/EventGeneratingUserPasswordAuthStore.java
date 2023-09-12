package io.appform.conductor.server.usermanagement;

import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.UserPasswordSetEvent;
import io.appform.conductor.server.internalmodels.auth.UserPasswordAuthDetails;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingUserPasswordAuthStore implements UserPasswordAuthStore {
    private final EventBus eventBus;
    private final UserPasswordAuthStore userPasswordAuthStore;

    @Inject
    public EventGeneratingUserPasswordAuthStore(EventBus eventBus, @Named("root") UserPasswordAuthStore userPasswordAuthStore) {
        this.eventBus = eventBus;
        this.userPasswordAuthStore = userPasswordAuthStore;
    }

    @Override
    public Optional<UserPasswordAuthDetails> set(String userId, String password) {
        val res = userPasswordAuthStore.set(userId, password);
        res.ifPresent(userPasswordAuthDetails -> eventBus.publish(new UserPasswordSetEvent(userPasswordAuthDetails.getUserId())));
        return res;
    }

    @Override
    public Optional<UserPasswordAuthDetails> update(String userId, UnaryOperator<UserPasswordAuthDetails> updater) {
        return userPasswordAuthStore.update(userId, updater);
    }

    @Override
    public Optional<UserPasswordAuthDetails> get(String userId) {
        return userPasswordAuthStore.get(userId);
    }
}
