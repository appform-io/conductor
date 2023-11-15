package io.appform.conductor.server.actionmanagement;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.actions.ActionCreatedEvent;
import io.appform.conductor.server.eventmanagement.events.actions.ActionDeletedEvent;
import io.appform.conductor.server.eventmanagement.events.actions.ActionUpdatedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingActionStore implements ActionStore {
    private final EventBus eventBus;
    private final ActionStore actionStore;

    @Inject
    public EventGeneratingActionStore(EventBus eventBus, @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) ActionStore actionStore) {
        this.eventBus = eventBus;
        this.actionStore = actionStore;
    }

    @Override
    public Optional<Action> read(String actionId) {
        return actionStore.read(actionId);
    }

    @Override
    public List<Action> read(List<String> actionId) {
        return actionStore.read(actionId);
    }

    @Override
    public Optional<Action> save(Action action) {
        val res = actionStore.save(action);
        res.ifPresent(actionCreated -> eventBus.publish(new ActionCreatedEvent(actionCreated.getId())));
        return res;
    }

    @Override
    public boolean update(String actionId, UnaryOperator<Action> handler) {
        val res = actionStore.update(actionId, handler);
        if (res) {
            eventBus.publish(new ActionUpdatedEvent(actionId));
        }
        return res;
    }

    @Override
    public List<Action> listActionsForScopes(Collection<Scope> scopes) {
        return actionStore.listActionsForScopes(scopes);
    }

    @Override
    public List<Action> listActionsForIds(Collection<String> actionIds) {
        return actionStore.listActionsForIds(actionIds);
    }

    @Override
    public boolean delete(String actionId) {
        val res = actionStore.delete(actionId);
        if (res) {
            eventBus.publish(new ActionDeletedEvent(actionId));
        }
        return res;
    }
}
