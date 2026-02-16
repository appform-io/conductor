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

package io.appform.conductor.core.interfaces;

import io.appform.conductor.model.skills.SkillDefinition;
import io.appform.conductor.model.skills.SkillValue;
import io.appform.conductor.model.usermgmt.*;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserLifecycleManager {
    Optional<User> userDetails(@NonNull final String userId);

    List<Group> listGroups();

    boolean addUserToGroup(String groupId, String userId);

    boolean removeUserFromGroup(String groupId, String userId);

    Optional<UserSummary> createSystemUser(String name, String email);

    Optional<UserSummary> createHumanUser(String name, String email, String password);

    Optional<UserSession> startSystemUserSession(final String userId);

    Optional<String> jwtForSession(String userId, String sessionId);

    boolean completeUserSession(final String userId, final String sessionId);

    List<SkillValue> listSkillValues();

    Optional<Group> createGroup(String name, String description, GroupType type, Set<String> requiredSkills);

    Optional<Group> readGroup(final String groupId);

    Optional<Group> updateGroup(
            final String groupId,
            final String description,
            final GroupType type,
            final Set<String> requiredSkills);

    boolean deleteGroup(String groupId);

    boolean addUserSkill(String userId, Skill skill);

    Optional<SkillDefinition> getSkill(final String skillId);

    List<SkillDefinition> listSkillDefinitions();

    boolean removeUserSkill(String userId, Skill skill);

    Optional<SkillDefinition> removeSkillValue(
            final String skillId,
            final String valueId);

    Optional<SkillDefinition> addSkillValue(
            final String skillId,
            final String value);

    boolean deleteSkillDefinition(final String skillId);

    Optional<SkillDefinition> createSkill(final String name);

    Optional<SkillDefinition> updateSkillDefinition(
            final String skillId,
            final String name);

    Optional<UserActivationToken> openToken(String userId);

    Optional<UserSummary> showToken(String token);

    Optional<UserSession> activateUser(String token, String password);

    Optional<UserSession> loginUser(String email, String password);

    boolean logoutUser(UserSession session);

    Optional<UserSummary> updateUserName(String userId, String name);

    boolean changePassword(String userId, String oldPassword, String newPassword);

    Optional<UserSession> validateToken(String token);

    List<UserSummary> findUsersForGroup(String groupId, int start, int limit);

    List<SkillValue> getSkillsForUser(String userId);
}
