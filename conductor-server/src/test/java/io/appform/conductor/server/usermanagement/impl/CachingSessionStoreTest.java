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

import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.core.hazelcast.HazelcastClient;
import io.appform.conductor.user.usermanagement.impl.CachingSessionStore;
import io.appform.conductor.user.usermanagement.impl.DBSessionStore;
import io.appform.conductor.user.usermanagement.impl.models.StoredUserSessionDetails;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.usermanagement.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
class CachingSessionStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void testFunctionality(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcastClient) {
        val dbStore = spy(new DBSessionStore(bundle.createRelatedObjectDao(StoredUserSessionDetails.class)));
        val store = new CachingSessionStore(dbStore, hazelcastClient);
        val opCtr = new AtomicInteger(0);

        doAnswer((Answer<Optional<UserSessionDetails>>)invocationMock -> {
            opCtr.incrementAndGet();
            return (Optional<UserSessionDetails>) invocationMock.callRealMethod();
        }).when(dbStore).getById(anyString(), anyString());

        val numSessions = 100;
        val sessions = IntStream.rangeClosed(1, numSessions)
                .mapToObj(i -> store.create("U1", SessionType.DYNAMIC, null).orElse(null))
                .sorted(Comparator.comparing(UserSessionDetails::getUserId).thenComparing(UserSessionDetails::getId))
                .toList();
        assertEquals(numSessions, opCtr.get());
        assertEquals(sessions, sessions.stream()
                .map(session -> store.getById(session.getUserId(), session.getId()).orElse(null))
                .filter(Objects::nonNull)
                .toList());
        assertTrue(sessions.stream()
                           .map(session -> store.getById(session.getUserId(), session.getId()).orElse(null))
                           .filter(Objects::nonNull)
                           .allMatch(session -> session.getState().equals(SessionState.ACTIVE)));
        assertEquals(numSessions, opCtr.get()); //No extra reads
        sessions.forEach(session -> store.complete(session.getUserId(), session.getId()));
        assertEquals(3 * numSessions, opCtr.get()); //One in update path, one in refresh
        assertTrue(sessions.stream()
                           .map(session -> store.getById(session.getUserId(), session.getId()).orElse(null))
                           .filter(Objects::nonNull)
                           .allMatch(session -> session.getState().equals(SessionState.COMPLETED)));
        assertEquals(3 * numSessions, opCtr.get()); //No extra reads

    }
}