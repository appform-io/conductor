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

package io.appform.conductor.user.usermanagement;

import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * A store for {@link UserSessionDetails}
 */
public interface SessionStore {

    Optional<UserSessionDetails> create(String userId, SessionType type, Date expiry);
    Optional<UserSessionDetails> getById(String userId, String sessionId);

    default List<UserSessionDetails> list(String userId) {
        return list(userId, EnumSet.allOf(SessionState.class));
    }

    List<UserSessionDetails> list(String userId, Set<SessionState> requiredStates);

    Optional<UserSessionDetails> update(String userId, String sessionId, UnaryOperator<UserSessionDetails> handler);

    default boolean complete(String userId, String sessionId) {
        return update(userId, sessionId,
                      userSessionDetails -> userSessionDetails.withState(SessionState.COMPLETED))
                .map(userSessionDetails -> userSessionDetails.getState().equals(SessionState.COMPLETED))
                .orElse(false);
    }

}
