/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.server.store;


import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.GroupDetails;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Storage layer for {@link Group} objects
 */
public interface GroupStore {

    Optional<GroupDetails> create(String name, String description);
    Optional<GroupDetails> get(String groupId);

    List<GroupDetails> get(List<String> groupIds);
    Optional<GroupDetails> delete(String groupId);
    Optional<GroupDetails> update(String groupId, Consumer<GroupDetails> handler);
    boolean addUserToGroup(String groupId, String userId);

    boolean removeUserFromGroup(String groupId, String userId);

    List<String> findUsersForGroup(String groupId, int start, int limit);
    List<GroupDetails> findGroupsForUser(String userId);
}
