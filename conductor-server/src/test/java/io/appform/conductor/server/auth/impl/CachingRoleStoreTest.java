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

import io.appform.conductor.model.auth.Role;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.auth.impl.models.StoredRole;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.auth.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
class CachingRoleStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void testFunctionality(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcastClient) {
        val dbStore = spy(new DBRoleStore(bundle.createParentObjectDao(StoredRole.class)));
        val dbCtr = new AtomicInteger(0);
        doAnswer((Answer<Optional<Role>>) invocationOnMock -> {
            dbCtr.incrementAndGet();
            return (Optional<Role>) invocationOnMock.callRealMethod();
        }).when(dbStore).create(anyString(), anyString(), anyString(), anySet());

        doAnswer((Answer<Optional<Role>>) invocationOnMock -> {
            dbCtr.incrementAndGet();
            return (Optional<Role>) invocationOnMock.callRealMethod();
        }).when(dbStore).update(anyString(), any());

        doAnswer((Answer<Boolean>) invocationOnMock -> {
            dbCtr.incrementAndGet();
            return (Boolean) invocationOnMock.callRealMethod();
        }).when(dbStore).delete(anyString());

        val store = new CachingRoleStore(dbStore, hazelcastClient);
        val numRoles = 100;
        val roles = IntStream.rangeClosed(1, numRoles)
                .mapToObj(i -> store.create("R-" + i, "role-" + i, "New ", Set.of()).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Role::getId))
                .toList();
        assertEquals(numRoles, dbCtr.get());
        assertEquals(roles, store.list()
                .stream()
                .sorted(Comparator.comparing(Role::getId))
                .toList());
        assertEquals(roles, roles.stream()
                .map(Role::getId)
                .map(rId -> store.read(rId).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Role::getId))
                .toList());
        assertEquals(numRoles, dbCtr.get()); //No db access
        roles.forEach(role -> store.update(role.getId(), r -> r.withDescription("Updated")));
        assertEquals(2 * numRoles, dbCtr.get());
        assertTrue(store.list()
                .stream()
                .allMatch(r -> r.getDescription().equals("Updated")));
        assertEquals(2 * numRoles, dbCtr.get()); //No db access
        assertTrue(roles.stream().allMatch(role -> store.delete(role.getId())));
    }
}