package io.appform.conductor.server.auth;

import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.user.UserRoleAssignedEvent;
import io.appform.conductor.server.eventmanagement.events.user.UserRoleRevokedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class EventGeneratingUserRoleMappingStore implements UserRoleMappingStore {
    private final EventBus eventBus;
    private final UserRoleMappingStore  userRoleMappingStore;

    @Inject
    public EventGeneratingUserRoleMappingStore(EventBus eventBus, @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) UserRoleMappingStore  userRoleMappingStore) {
        this.eventBus = eventBus;
        this.userRoleMappingStore = userRoleMappingStore;
    }

    @Override
    public boolean assignRoleToUser(String userId, String roleId) {
        val res = userRoleMappingStore.assignRoleToUser(userId, roleId);
        if(res) {
            eventBus.publish(new UserRoleAssignedEvent(userId, roleId));
        }
        return res;
    }

    @Override
    public boolean revokeRoleFromUser(String userId, String roleId) {
        val res = userRoleMappingStore.revokeRoleFromUser(userId, roleId);
        if(res) {
            eventBus.publish(new UserRoleRevokedEvent(userId, roleId));
        }
        return res;
    }

    @Override
    public Optional<String> roleForUser(String userId) {
        return userRoleMappingStore.roleForUser(userId);
    }
}
