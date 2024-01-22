package io.appform.conductor.server.auth;

import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.auth.Role;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.model.events.impl.role.RoleCreatedEvent;
import io.appform.conductor.model.events.impl.role.RoleDeletedEvent;
import io.appform.conductor.model.events.impl.role.RoleUpdatedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingRoleStore implements RoleStore {

    private final EventBus eventBus;
    private final RoleStore roleStore;

    @Inject
    public EventGeneratingRoleStore(EventBus eventBus, @Named(ConductorModule.CACHED_IMPLEMENTATION_NAME) RoleStore roleStore) {
        this.eventBus = eventBus;
        this.roleStore = roleStore;
    }

    @Override
    public Optional<Role> create(String roleId, String name, String description, Set<Permission> permissions) {
        val res = roleStore.create(roleId, name, description, permissions);
        res.ifPresent(role -> eventBus.publish(new RoleCreatedEvent(role.getId())));
        return res;
    }

    @Override
    public Optional<Role> read(String roleId) {
        return roleStore.read(roleId);
    }

    @Override
    public List<Role> list() {
        return roleStore.list();
    }

    @Override
    public Optional<Role> update(String roleId, UnaryOperator<Role> handler) {
        val res = roleStore.update(roleId, handler);
        res.ifPresent(role -> eventBus.publish(new RoleUpdatedEvent(role.getId())));
        return res;
    }

    @Override
    public boolean delete(String roleId) {
        val res = roleStore.delete(roleId);
        if(res) {
            eventBus.publish(new RoleDeletedEvent(roleId));
        }
        return res;
    }
}
