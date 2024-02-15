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

import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.usermanagement.impl.models.StoredGroup;
import io.appform.conductor.server.usermanagement.impl.models.StoredGroupUserMapping;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.NonNull;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
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
@RelevantDBEntityPackages("io.appform.conductor.server.usermanagement.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
class CachingGroupStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void testFunctionality(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcastClient) {
        val groupCallCount = new AtomicInteger(0);
        val userGroupsCallCount = new AtomicInteger(0);
        val dbDao = spy(new DBGroupStore(bundle.createParentObjectDao(StoredGroup.class),
                                         bundle.createRelatedObjectDao(StoredGroupUserMapping.class)));
        val store = new CachingGroupStore(dbDao, hazelcastClient);
        doAnswer(doDBForwarding(groupCallCount)).when(dbDao).create(anyString(), anyString(), any(), anySet());
        doAnswer(doDBForwarding(groupCallCount)).when(dbDao).read(anyString());
        doAnswer(doDBForwarding(groupCallCount)).when(dbDao).update(anyString(), any());
        doAnswer(callGrpAssocMethods(userGroupsCallCount)).when(dbDao).addUserToGroup(anyString(), anyString());
        doAnswer(callGrpAssocMethods(userGroupsCallCount)).when(dbDao).removeUserFromGroup(anyString(), anyString());
        doAnswer((Answer<List<Group>>) invocationOnMock -> {
            groupCallCount.incrementAndGet();
            return (List<Group>) invocationOnMock.callRealMethod();
        }).when(dbDao).findGroupsForUser(anyString());

        val numGroups = 100;
        val groups = IntStream.rangeClosed(1, numGroups)
                .mapToObj(i -> store.create("Test-" + i, "Test group " + i, GroupType.MANUALLY_ASSIGNED, Set.of())
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Group::getId))
                .toList();
        assertEquals(2 * numGroups, groupCallCount.get()); //For creates and refreshes
        assertEquals(groups, groups.stream()
                .map(Group::getId)
                .map(gId -> store.read(gId).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Group::getId))
                .toList()); //This will cause the first load
        assertEquals(2 * numGroups, groupCallCount.get()); //create + read
        assertEquals(groups, store.read(groups.stream()
                                                .map(Group::getId)
                                                .toList())
                .stream()
                .sorted(Comparator.comparing(Group::getId))
                .toList());
        assertEquals(2 * numGroups, groupCallCount.get()); //No extra reads are being done
        assertEquals(groups, store.list().stream().sorted(Comparator.comparing(Group::getId)).toList());
        assertEquals(numGroups, groups.stream()
                .map(grp -> store.update(grp.getId(), g -> g.withDescription("Updated")).orElse(null))
                .filter(Objects::nonNull)
                .filter(g -> g.getDescription().equals("Updated"))
                .count());

        val gids = groups.stream()
                .map(Group::getId)
                .sorted()
                .toList();
        assertTrue(gids.stream().allMatch(gid -> store.addUserToGroup(gid, "U1")));
        assertEquals(numGroups, userGroupsCallCount.get());
        assertEquals(gids, store.findGroupsForUser("U1").stream().map(Group::getId).sorted().toList());
        assertEquals(gids, store.findGroupsForUser("U1").stream().map(Group::getId).sorted().toList());
        assertTrue(store.findGroupsForUser("U2").isEmpty());
        assertEquals(numGroups, userGroupsCallCount.get()); //Multiple reads, but db calls count doesn't increase
        assertTrue(gids.stream().allMatch(gid -> store.removeUserFromGroup(gid, "U1")));
        assertTrue(store.findGroupsForUser("U1").isEmpty());
        assertTrue(store.findGroupsForUser("U1").isEmpty());
        assertEquals(2 * numGroups, userGroupsCallCount.get()); //Multiple reads, but db calls count doesn't increase

        assertTrue(gids.stream().allMatch(store::delete));
        assertTrue(store.list().isEmpty());

    }

    @NonNull
    private static Answer<Boolean> callGrpAssocMethods(AtomicInteger userGroupsCallCount) {
        return new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                userGroupsCallCount.incrementAndGet();
                return (boolean) invocationOnMock.callRealMethod();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Answer<Optional<Group>> doDBForwarding(AtomicInteger groupCallCount) {
        return invocationOnMock -> {
            groupCallCount.incrementAndGet();
            return (Optional<Group>) invocationOnMock.callRealMethod();
        };
    }
}