package io.appform.conductor.server.usermanagement;

import io.appform.conductor.model.events.impl.group.GroupCreatedEvent;
import io.appform.conductor.model.events.impl.group.GroupDeletedEvent;
import io.appform.conductor.model.events.impl.group.GroupUpdatedEvent;
import io.appform.conductor.model.events.impl.user.UserGroupAssignedEvent;
import io.appform.conductor.model.events.impl.user.UserGroupUnassignedEvent;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingGroupStore implements GroupStore {
    private final EventBus eventBus;
    private final GroupStore groupStore;

    @Inject
    public EventGeneratingGroupStore(EventBus eventBus, @Named(ConductorModule.CACHED_IMPLEMENTATION_NAME) GroupStore groupStore) {
        this.eventBus = eventBus;
        this.groupStore = groupStore;
    }

    @Override
    public Optional<Group> create(String name, String description, GroupType type, Set<String> requiredSkills) {
        val res = groupStore.create(name, description, type, requiredSkills);
        res.ifPresent(group -> eventBus.publish(new GroupCreatedEvent(group.getId())));
        return res;
    }

    @Override
    public Optional<Group> read(String groupId) {
        return groupStore.read(groupId);
    }

    @Override
    public List<Group> read(List<String> groupIds) {
        return groupStore.read(groupIds);
    }

    @Override
    public boolean delete(String groupId) {
        val status = groupStore.delete(groupId);
        if(status) {
            eventBus.publish(new GroupDeletedEvent(groupId));
        }
        return status;
    }

    @Override
    public Optional<Group> update(String groupId, UnaryOperator<Group> handler) {
        val res = groupStore.update(groupId, handler);
        res.ifPresent(group -> eventBus.publish(new GroupUpdatedEvent(group.getId())));
        return res;
    }

    @Override
    public boolean addUserToGroup(String groupId, String userId) {
        val res = groupStore.addUserToGroup(groupId, userId);
        if(res) {
            eventBus.publish(new UserGroupAssignedEvent(groupId, userId));
        }
        return res;
    }

    @Override
    public boolean removeUserFromGroup(String groupId, String userId) {
        val res = groupStore.removeUserFromGroup(groupId, userId);
        if(res) {
            eventBus.publish(new UserGroupUnassignedEvent(groupId, userId));
        }
        return res;
    }

    @Override
    public List<String> findUsersForGroup(String groupId, int start, int limit) {
        return groupStore.findUsersForGroup(groupId, start, limit);
    }

    @Override
    public List<Group> findGroupsForUser(String userId) {
        return groupStore.findGroupsForUser(userId);
    }

    @Override
    public List<Group> list() {
        return groupStore.list();
    }
}