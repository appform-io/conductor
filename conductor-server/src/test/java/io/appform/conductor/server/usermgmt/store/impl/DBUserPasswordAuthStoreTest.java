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

package io.appform.conductor.server.usermgmt.store.impl;

import io.appform.conductor.server.DBTestBase;
import io.appform.conductor.server.internalmodels.auth.UserPasswordAuthDetails;
import io.appform.conductor.server.usermanagement.impl.DBUserPasswordAuthStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserPassword;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class DBUserPasswordAuthStoreTest extends DBTestBase {
    @Test
    void test() {
        val store =
                new DBUserPasswordAuthStore(bundle.createRelatedObjectDao(StoredUserPassword.class));
        val res = store.set("TEST_USER", "TEST_PASSWORD");
        assertTrue(res.isPresent());
        assertEquals(res.map(UserPasswordAuthDetails::getPassword).orElse(null),
                     store.get("TEST_USER").map(UserPasswordAuthDetails::getPassword).orElse(null));
        assertEquals(0, res.map(UserPasswordAuthDetails::getFailedPasswordAttempts).orElse(-1));
        assertEquals(5, store.update("TEST_USER",
                                     existing -> new UserPasswordAuthDetails(existing.getUserId(),
                                                                             existing.getPassword(),
                                                                             5,
                                                                             existing.getCreated(),
                                                                             new Date()))
                .map(UserPasswordAuthDetails::getFailedPasswordAttempts)
                .orElse(-1));
    }
}