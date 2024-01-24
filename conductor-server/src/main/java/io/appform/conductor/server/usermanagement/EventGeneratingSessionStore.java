package io.appform.conductor.server.usermanagement;

import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.model.events.impl.user.UserSessionCreatedEvent;
import io.appform.conductor.model.events.impl.user.UserSessionUpdatedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Singleton
public class EventGeneratingSessionStore implements SessionStore {

    private final EventBus eventBus;
    private final SessionStore sessionStore;

    @Inject
    public EventGeneratingSessionStore(EventBus eventBus, @Named(ConductorModule.CACHED_IMPLEMENTATION_NAME) SessionStore sessionStore) {
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
    public Optional<UserSessionDetails> update(String userId, String sessionId, Function<UserSessionDetails, UserSessionDetails> handler) {
        val res = sessionStore.update(userId, sessionId, handler);
        res.ifPresent(userSessionDetails -> eventBus.publish(new UserSessionUpdatedEvent(
                userSessionDetails.getId(),
                userSessionDetails.getUserId(),
                userSessionDetails.getState())));
        return res;
    }
}
