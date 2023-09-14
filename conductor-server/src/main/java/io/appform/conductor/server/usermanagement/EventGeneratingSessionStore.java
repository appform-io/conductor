package io.appform.conductor.server.usermanagement;

import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.UserSessionCreatedEvent;
import io.appform.conductor.server.eventmanagement.events.UserSessionUpdatedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class EventGeneratingSessionStore implements SessionStore {

    private final EventBus eventBus;
    private final SessionStore sessionStore;

    @Inject
    public EventGeneratingSessionStore(EventBus eventBus, @Named("root") SessionStore sessionStore) {
        this.eventBus = eventBus;
        this.sessionStore = sessionStore;
    }

    @Override
    public Optional<UserSessionDetails> create(String userId, SessionType type, Date expiry) {
        val res = sessionStore.create(userId, type, expiry);
        res.ifPresent(userSessionDetails -> eventBus.publish(new UserSessionCreatedEvent(userSessionDetails.getId(),
                userSessionDetails.getUserId())));
        return res;
    }

    @Override
    public Optional<UserSessionDetails> getById(String userId, String sessionId) {
        return sessionStore.getById(userId, sessionId);
    }

    @Override
    public Optional<UserSessionDetails> update(String userId, String sessionId, Consumer<UserSessionDetails> handler) {
        val res = sessionStore.update(userId, sessionId, handler);
        res.ifPresent(userSessionDetails -> eventBus.publish(new UserSessionUpdatedEvent(userSessionDetails.getId(),
                userSessionDetails.getUserId())));
        return res;
    }
}
