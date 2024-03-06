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

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.usermanagement.impl.models.StoredUserSessionDetails;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DBSessionStore}
 */
@Slf4j
@RelevantDBEntityPackages("io.appform.conductor.server.usermanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBSessionStoreTest {

    @Test
    void testCreate(BalancedDBShardingBundle<TestConfig> bundle) {
        val userStore = new DBSessionStore(createRealUserDao(bundle));
        val newUserSession = userStore.create("user_id",
                                              SessionType.DYNAMIC,
                                              Date.from(Instant.now().plus(Duration.ofDays(7)))).orElse(null);
        assertNotNull(newUserSession);
        assertEquals("user_id", newUserSession.getUserId());
        assertEquals(SessionState.ACTIVE, newUserSession.getState());
    }

    @Test
    @SneakyThrows
    void testCreateFailed(BalancedDBShardingBundle<TestConfig> bundle) {
        val userDao = createMockUserDao(bundle);
        val mockUserDao = new DBSessionStore(userDao);
        doThrow(NullPointerException.class).when(userDao).save(anyString(), any(StoredUserSessionDetails.class));
        try {
            mockUserDao.create("user_id", SessionType.DYNAMIC, Date.from(Instant.now().plus(Duration.ofDays(7))));
            fail("Should have thrown exception");
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("Error,:", e);
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException) e).getErrorCode());
        }
    }

    private RelationalDao<StoredUserSessionDetails> createRealUserDao(BalancedDBShardingBundle<TestConfig> bundle) {
        return bundle.createRelatedObjectDao(StoredUserSessionDetails.class);
    }

    @SuppressWarnings("unchecked")
    private RelationalDao<StoredUserSessionDetails> createMockUserDao(BalancedDBShardingBundle<TestConfig> bundle) {
        return (RelationalDao<StoredUserSessionDetails>) mock(RelationalDao.class);
    }
}