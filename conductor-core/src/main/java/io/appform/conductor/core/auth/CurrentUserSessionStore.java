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

package io.appform.conductor.core.auth;

import io.appform.conductor.model.usermgmt.UserSession;
import lombok.experimental.UtilityClass;

import java.util.Optional;

/**
 *
 */
@UtilityClass
public class CurrentUserSessionStore {
    private static final ThreadLocal<UserSession> currentSession = new ThreadLocal<>();

    public static void set(UserSession session) {
        currentSession.set(session);
    }

    public static Optional<UserSession> get() {
        return Optional.ofNullable(currentSession.get());
    }

    public static void clear() {
        currentSession.remove();
    }
}
