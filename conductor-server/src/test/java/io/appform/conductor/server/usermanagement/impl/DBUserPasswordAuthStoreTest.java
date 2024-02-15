/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.server.usermanagement.impl;

import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.internalmodels.auth.UserPasswordAuthDetails;
import io.appform.conductor.server.usermanagement.impl.DBUserPasswordAuthStore;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserPassword;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DBUserPasswordAuthStore}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.usermanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBUserPasswordAuthStoreTest {
    @Test
    void test(BalancedDBShardingBundle<TestConfig> bundle) {
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