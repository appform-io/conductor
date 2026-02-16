package io.appform.conductor.user.auth;

import io.appform.conductor.core.auth.UserRoleMappingStore;
import io.appform.conductor.core.eventmanagement.EventBus;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.model.events.impl.user.UserRoleAssignedEvent;
import io.appform.conductor.model.events.impl.user.UserRoleRevokedEvent;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class EventGeneratingUserRoleMappingStore implements UserRoleMappingStore {
    private final EventBus eventBus;
    private final UserRoleMappingStore userRoleMappingStore;

    @Inject
    public EventGeneratingUserRoleMappingStore(
            EventBus eventBus,
            @Named(Constants.CACHED_IMPLEMENTATION_NAME) UserRoleMappingStore userRoleMappingStore) {
        this.eventBus = eventBus;
        this.userRoleMappingStore = userRoleMappingStore;
    }

    @Override
    public boolean assignRoleToUser(String userId, String roleId) {
        val res = userRoleMappingStore.assignRoleToUser(userId, roleId);
        if (res) {
            eventBus.publish(new UserRoleAssignedEvent(userId, roleId));
        }
        return res;
    }

    @Override
    public boolean revokeRoleFromUser(String userId, String roleId) {
        val res = userRoleMappingStore.revokeRoleFromUser(userId, roleId);
        if (res) {
            eventBus.publish(new UserRoleRevokedEvent(userId, roleId));
        }
        return res;
    }

    @Override
    public Optional<String> roleForUser(String userId) {
        return userRoleMappingStore.roleForUser(userId);
    }
}
