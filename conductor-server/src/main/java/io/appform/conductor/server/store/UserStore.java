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

import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 *
 */
public interface UserStore {

    Optional<UserSummary> create(String name, UserType type, String email);
    Optional<UserSummary> getById(String userId);
    List<UserSummary> getByIds(List<String> userIds);
    Optional<UserSummary> getByEmail(String email);
    Optional<UserSummary> update(String userId, UnaryOperator<UserSummary> handler);

    default Optional<UserSummary> updateState(final String userId, final UserState state) {
        return update(userId, existing -> new UserSummary(existing.getId(),
                                              existing.getType(),
                                              existing.getName(),
                                              existing.getEmail(),
                                              state,
                                              existing.getCreated(),
                                              new Date()));
    }
}
