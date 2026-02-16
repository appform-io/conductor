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

import io.appform.conductor.model.usermgmt.UserActivationToken;
import io.appform.conductor.model.usermgmt.UserActivationTokenState;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 */
public interface UserActivationTokenStore {

    Optional<UserActivationToken> generate(String userId, Date validTill);
    Optional<UserActivationToken> getById(String token);

    Optional<UserActivationToken> getForUser(String userId, Set<UserActivationTokenState> requiredStates);

    boolean update(String token, Consumer<UserActivationToken> handler);

    default boolean updateTokenState(String token, UserActivationTokenState userActivationTokenState) {
        return update(token, t -> t.setState(userActivationTokenState));
    }
}
