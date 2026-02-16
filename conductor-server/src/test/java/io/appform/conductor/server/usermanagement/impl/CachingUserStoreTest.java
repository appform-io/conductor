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

import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.core.hazelcast.HazelcastClient;
import io.appform.conductor.user.usermanagement.impl.DBUserStore;
import io.appform.conductor.user.usermanagement.impl.models.StoredUser;
import io.appform.conductor.user.usermanagement.impl.CachingUserStore;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.usermanagement.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
class CachingUserStoreTest {
    @Test
    @SuppressWarnings("unchecked")
    void test(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hz) {
        val root = spy(new DBUserStore(bundle.createParentObjectDao(StoredUser.class)));
        val getCounter = new AtomicInteger(0);
        doAnswer((Answer<Optional<UserSummary>>) invocationOnMock -> {
            getCounter.incrementAndGet();
            return (Optional<UserSummary>) invocationOnMock.callRealMethod();
        }).when(root).getById(anyString());
        val store = new CachingUserStore(root, hz);
        val numUsers = 100;
        val users = IntStream.rangeClosed(1, numUsers)
                .mapToObj(i -> store.create(String.format("U%03d", i),
                                            "User " + i,
                                            UserType.HUMAN,
                                            "u_" + i + "@blah.blah").orElse(null))
                .toList();
        assertEquals(numUsers, users.size());
        IntStream.rangeClosed(1, 10) //Scan 10 times, but read only once
                .forEach(i -> users.forEach(user -> {
                    assertEquals(user, store.getById(user.getId()).orElse(null));
                    assertEquals(UserState.CREATED,
                                 store.getById(user.getId()).map(UserSummary::getState).orElse(null));
                }));
        assertEquals(users.stream().sorted(Comparator.comparing(UserSummary::getId)).toList(),
                     store.getByIds(users.stream().map(UserSummary::getId).toList())
                             .stream().sorted(Comparator.comparing(UserSummary::getId)).toList());
        assertEquals(numUsers, getCounter.get());

        users.forEach(user -> store.updateState(user.getId(), UserState.ACTIVE)); //This will increment counter by 100 + 100 (getById inside update on db store)
        assertEquals(3 * numUsers, getCounter.get());

        IntStream.rangeClosed(1, 10) //Scan 10 times, but read only once
                .forEach(i -> users.forEach(user -> assertEquals(UserState.ACTIVE,
                                                             store.getById(user.getId())
                                                           .map(UserSummary::getState)
                                                           .orElse(null))));
        assertEquals(3 * numUsers, getCounter.get());
    }
}