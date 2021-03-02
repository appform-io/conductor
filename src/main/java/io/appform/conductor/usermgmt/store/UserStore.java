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

package io.appform.conductor.usermgmt.store;

import io.appform.conductor.usermgmt.model.UserState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *
 */
public interface UserStore {

    @Data
    @AllArgsConstructor
    class UserDetails {
        private final String id;
        private String name;
        private String email;
        private String password;
        private UserState state;
        private int failedPasswordAttempts;
        private final Date created;
        private final Date updated;
    }

    Optional<UserDetails> create(String name, String email, String password);
    Optional<UserDetails> getById(String userId);
    List<UserDetails> getByIds(List<String> userIds);
    Optional<UserDetails> getByEmail(String userId);
    Optional<UserDetails> update(String userId, Consumer<UserDetails> handler);
}
