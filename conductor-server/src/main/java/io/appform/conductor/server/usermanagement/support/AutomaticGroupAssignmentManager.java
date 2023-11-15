/*
 * Copyright (c) 2023 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.usermanagement.support;

import io.appform.conductor.model.skills.SkillValue;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.server.eventmanagement.*;
import io.appform.conductor.server.eventmanagement.events.user.UserSkillAssociatedEvent;
import io.appform.conductor.server.eventmanagement.events.user.UserSkillDisasocciatedEvent;
import io.appform.conductor.server.usermanagement.UserLifecycleManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@EventHandlerImplementation
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class AutomaticGroupAssignmentManager implements EventHandler {
    private final UserLifecycleManager lifecycleManager;

    @Override
    public void handle(Event event) {
        event.accept(new EventVisitorAdapter<Void>() {
            @Override
            public Void visit(UserSkillAssociatedEvent userSkillAssociatedEvent) {
                evaluateGroupAssignmentsForUser(userSkillAssociatedEvent.getUserId());
                return super.visit(userSkillAssociatedEvent);
            }

            @Override
            public Void visit(UserSkillDisasocciatedEvent userSkillDisasocciatedEvent) {
                evaluateGroupAssignmentsForUser(userSkillDisasocciatedEvent.getUserId());
                return super.visit(userSkillDisasocciatedEvent);
            }
        });
    }

    @Override
    public Set<EventType> listenFor() {
        return EnumSet.of(EventType.USER_SKILL_ASSOCIATED, EventType.USER_SKILL_DISACCOSIATED);
    }

    private void evaluateGroupAssignmentsForUser(final String userId) {

        val user = lifecycleManager.userDetails(userId).orElse(null);
        if (null == user) {
            log.warn("Ignored skill assignment event for non existent user: {}", userId);
            return;
        }
        val userSkills = Objects.requireNonNullElse(user.getSkills(), List.<SkillValue>of())
                .stream()
                .map(SkillValue::getSkillValueId)
                .collect(Collectors.toUnmodifiableSet());

        lifecycleManager.listGroups()
                .stream()
                .filter(group -> GroupType.AUTOMATICALLY_ASSIGNED.equals(group.getType()))
                .filter(group -> userSkills.containsAll(group.getRequiredSkills()))
                .forEach(group -> {
                    if (lifecycleManager.addUserToGroup(group.getId(), userId)) {
                        log.info("Added user {} to group {}", userId, group.getId());
                    }
                    else {
                        log.error("Failed to add user {} to group {}", userId, group.getId());
                    }
                });
        Objects.requireNonNullElse(user.getGroups(), List.<Group>of())
                .stream()
                .filter(group -> GroupType.AUTOMATICALLY_ASSIGNED.equals(group.getType()))
                .filter(group -> !userSkills.containsAll(group.getRequiredSkills()))
                .forEach(group -> {
                    if (lifecycleManager.removeUserFromGroup(group.getId(), userId)) {
                        log.info("Removed user {} from group {}", userId, group.getId());
                    }
                    else {
                        log.error("Failed to remove user {} from group {}", userId, group.getId());
                    }
                });
    }
}
