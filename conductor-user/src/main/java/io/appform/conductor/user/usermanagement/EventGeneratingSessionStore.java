package io.appform.conductor.user.usermanagement;

import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.model.events.impl.user.UserSessionCreatedEvent;
import io.appform.conductor.model.events.impl.user.UserSessionUpdatedEvent;
import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import static io.appform.conductor.core.utils.Constants.ROOT_IMPLEMENTATION_NAME;
import io.appform.conductor.core.eventmanagement.EventBus;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingSessionStore implements SessionStore {

    private final EventBus eventBus;
    private final SessionStore sessionStore;

    @Inject
    public EventGeneratingSessionStore(EventBus eventBus, @Named(Constants.CACHED_IMPLEMENTATION_NAME) SessionStore sessionStore) {
        this.eventBus = eventBus;
        this.sessionStore = sessionStore;
    }

    @Override
    public Optional<UserSessionDetails> create(String userId, SessionType type, Date expiry) {
        val res = sessionStore.create(userId, type, expiry);
        res.ifPresent(userSessionDetails -> eventBus.publish(new UserSessionCreatedEvent(
                userSessionDetails.getId(),
                userSessionDetails.getUserId(),
                userSessionDetails.getType())));
        return res;
    }

    @Override
    public Optional<UserSessionDetails> getById(String userId, String sessionId) {
        return sessionStore.getById(userId, sessionId);
    }

    @Override
    public List<UserSessionDetails> list(String userId, Set<SessionState> requiredStates) {
        return sessionStore.list(userId, requiredStates);
    }

    @Override
    public Optional<UserSessionDetails> update(String userId, String sessionId, UnaryOperator<UserSessionDetails> handler) {
        val res = sessionStore.update(userId, sessionId, handler);
        res.ifPresent(userSessionDetails -> eventBus.publish(new UserSessionUpdatedEvent(
                userSessionDetails.getId(),
                userSessionDetails.getUserId(),
                userSessionDetails.getState())));
        return res;
    }
}
