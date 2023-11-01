package io.appform.conductor.server.usermanagement;

import io.appform.conductor.model.usermgmt.UserActivationToken;
import io.appform.conductor.model.usermgmt.UserActivationTokenState;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.UserActivationTokenGeneratedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Singleton
public class EventGeneratingUserActivationTokenStore implements UserActivationTokenStore {
    private EventBus eventBus;
    private UserActivationTokenStore userActivationTokenStore;

    @Inject
    public EventGeneratingUserActivationTokenStore(EventBus eventBus, @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) UserActivationTokenStore userActivationTokenStore) {
        this.eventBus = eventBus;
        this.userActivationTokenStore = userActivationTokenStore;
    }

    @Override
    public Optional<UserActivationToken> generate(String userId, Date validTill) {
        val res = userActivationTokenStore.generate(userId, validTill);
        res.ifPresent(userActivationToken -> eventBus.publish(new UserActivationTokenGeneratedEvent(
                userActivationToken.getUserId())));
        return res;
    }

    @Override
    public Optional<UserActivationToken> getById(String token) {
        return userActivationTokenStore.getById(token);
    }

    @Override
    public Optional<UserActivationToken> getForUser(String userId, Set<UserActivationTokenState> requiredStates) {
        return userActivationTokenStore.getForUser(userId, requiredStates);
    }

    @Override
    public boolean update(String token, Consumer<UserActivationToken> handler) {
        return userActivationTokenStore.update(token, handler);
    }
}
