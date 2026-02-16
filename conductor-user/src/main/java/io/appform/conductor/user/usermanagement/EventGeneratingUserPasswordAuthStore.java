package io.appform.conductor.user.usermanagement;

import io.appform.conductor.model.events.impl.user.UserPasswordFailedAttemptUpdatedEvent;
import static io.appform.conductor.core.utils.Constants.ROOT_IMPLEMENTATION_NAME;
import io.appform.conductor.core.eventmanagement.EventBus;
import io.appform.conductor.model.events.impl.user.UserPasswordSetEvent;
import io.appform.conductor.user.internalmodels.auth.UserPasswordAuthDetails;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingUserPasswordAuthStore implements UserPasswordAuthStore {
    private final EventBus eventBus;
    private final UserPasswordAuthStore userPasswordAuthStore;

    @Inject
    public EventGeneratingUserPasswordAuthStore(EventBus eventBus, @Named(ROOT_IMPLEMENTATION_NAME) UserPasswordAuthStore userPasswordAuthStore) {
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

    public Optional<UserPasswordAuthDetails> updateFailedPasswordAttempt(final String userId, ToIntFunction<Integer> attempts) {
        val res = userPasswordAuthStore.updateFailedPasswordAttempt(userId, attempts);
        res.ifPresent(userPasswordAuthDetails -> eventBus.publish(new UserPasswordFailedAttemptUpdatedEvent(userPasswordAuthDetails.getUserId(),
                userPasswordAuthDetails.getFailedPasswordAttempts())));
        return res;
    }

    @Override
    public Optional<UserPasswordAuthDetails> get(String userId) {
        return userPasswordAuthStore.get(userId);
    }
}
