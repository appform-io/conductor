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

package io.appform.conductor.server.auth.impl;

import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.auth.impl.models.StoredUserRoleMapping;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.auth.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
class CachingUserRoleMappingStoreTest {
    @Test
    @SuppressWarnings("unchecked")
    void testFunctionality(
            final BalancedDBShardingBundle<TestConfig> bundle,
            final HazelcastClient hazelcastClient) {
        val dbStore = spy(new DBUserRoleMappingStore(bundle.createRelatedObjectDao(StoredUserRoleMapping.class)));
        val store = new CachingUserRoleMappingStore(dbStore, hazelcastClient);

        val opCtr = new AtomicInteger(1);
        doAnswer((Answer<Optional<String>>)invocationMock -> {
            opCtr.incrementAndGet();
            return (Optional<String>) invocationMock.callRealMethod();
        }).when(dbStore).roleForUser(anyString());

        store.assignRoleToUser("U1", "R1");
        assertEquals(1, opCtr.get());
        IntStream.rangeClosed(1, 100).forEach(i -> {
            assertEquals("R1", store.roleForUser("U1").orElse(null));
        });
        assertEquals(1, opCtr.get());
        assertNull(store.roleForUser("U2").orElse(null));
        store.revokeRoleFromUser("U1", "R1");
        assertNull(store.roleForUser("U1").orElse(null));
    }
}