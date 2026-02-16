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
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.user.usermanagement.impl.DBUserStore;
import io.appform.conductor.user.usermanagement.impl.models.StoredUser;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DBUserStore}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.usermanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBUserStoreTest {

    @Test
    void testCreate(BalancedDBShardingBundle<TestConfig> bundle) {
        val userStore = new DBUserStore(createRealUserDao(bundle));
        val createdUser = userStore.create("test", "Test", UserType.HUMAN, "test@test.com")
                .orElse(null);
        assertNotNull(createdUser);
        assertEquals("Test", createdUser.getName());
        assertEquals("test@test.com", createdUser.getEmail());
    }

    @Test
    void testGetById(BalancedDBShardingBundle<TestConfig> bundle) {
        val userStore = new DBUserStore(createRealUserDao(bundle));
        val newUser = userStore.create("test", "Test", UserType.HUMAN, "test@test.com")
                .orElse(null);
        assertNotNull(newUser);
        val newId = newUser.getId();
        val returnedUser = userStore.getById(newId).orElse(null);
        assertNotNull(returnedUser);
        assertEquals(newUser, returnedUser);
    }

    @Test
    void testGetByIds(BalancedDBShardingBundle<TestConfig> bundle) {
        val numOfUsers = 10;
        val userStore = new DBUserStore(createRealUserDao(bundle));
        List<String> queryIds = new ArrayList<>();
        val users = new ArrayList<UserSummary>();
        IntStream.range(0, numOfUsers).forEach(
                i -> {
                    val newUser = userStore.create("test" + i, "Test" + i, UserType.HUMAN, "test" + i + "@gmail.com")
                            .orElse(null);
                    assertNotNull(newUser);
                    val newId = newUser.getId();
                    users.add(newUser);
                    queryIds.add(newId);
                }
                                              );

        val returnedUsers = userStore.getByIds(queryIds);
        assertEquals(numOfUsers, queryIds.size());
        assertEquals(numOfUsers, returnedUsers.size());
        IntStream.range(0, numOfUsers).forEach(
                i -> assertTrue(returnedUsers.contains(users.get(i)))
                                              );
    }

    @Test
    void testGetByEmailFailed(BalancedDBShardingBundle<TestConfig> bundle) {
        //User not found
        val userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        val emptyList = new ArrayList<UserSummary>();
        doReturn(emptyList).when(userDao).scatterGather(any(DetachedCriteria.class));
        val user = userStore.getByEmail("testMail").orElse(null);
        assertNull(user);

        //scatterGather throws exception
        val userDao2 = createMockUserDao();
        val userStore2 = new DBUserStore(userDao2);
        doThrow(NullPointerException.class).when(userDao2).scatterGather(any(DetachedCriteria.class));
        try {
            userStore2.getByEmail("testMail");
            fail("Should have thrown exception.");
        }
        catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_QUERY_ERROR, ((ConductorException) e).getErrorCode());
        }
    }

    @Test
    void testGetByEmail(BalancedDBShardingBundle<TestConfig> bundle) {
        val userStore = new DBUserStore(createRealUserDao(bundle));
        val newUser = userStore.create("test", "Test", UserType.HUMAN, "test@test.com")
                .orElse(null);
        assertNotNull(newUser);
        val newEmail = newUser.getEmail();
        val returnedUser = userStore.getByEmail(newEmail).orElse(null);
        assertNotNull(returnedUser);
        assertEquals(newUser, returnedUser);
    }

    @Test
    void testUpdate(BalancedDBShardingBundle<TestConfig> bundle) {
        val userStore = new DBUserStore(createRealUserDao(bundle));
        val newUser1 = userStore.create("test1", "Test1", UserType.HUMAN, "test1@test.com").orElse(null);
        val newUser2 = userStore.create("test2", "Test2", UserType.HUMAN, "test2@test.com").orElse(null);
        assertNotNull(newUser1);
        assertNotNull(newUser2);
        val newId1 = newUser1.getId();
        val newId2 = newUser2.getId();
        val returnedUser1 = userStore.getById(newId1).orElse(null);
        val returnedUser2 = userStore.getById(newId2).orElse(null);
        assertNotNull(returnedUser1);
        assertNotNull(returnedUser2);
        //Check Correct users are created
        assertEquals("Test1", returnedUser1.getName());
        assertEquals("test1@test.com", returnedUser1.getEmail());

        assertEquals("Test2", returnedUser2.getName());
        assertEquals("test2@test.com", returnedUser2.getEmail());
        val updatedUser = userStore.update(newId2,
                                           myUser -> new UserSummary(
                                                   myUser.getId(),
                                                   myUser.getType(),
                                                   "newTest",
                                                   "newTest@test.com",
                                                   UserState.INACTIVE,
                                                   myUser.getCreated(),
                                                   new Date())).orElse(null);
        assertNotNull(updatedUser);
        val returnedAgainUser1 = userStore.getById(newId1).orElse(null);
        val returnedAgainUser2 = userStore.getById(newId2).orElse(null);
        assertNotNull(returnedAgainUser1);
        assertNotNull(returnedAgainUser2);

        assertEquals("Test1", returnedAgainUser1.getName());
        assertEquals("test1@test.com", returnedAgainUser1.getEmail());

        assertEquals("newTest", returnedAgainUser2.getName());
        assertEquals("newTest@test.com", returnedAgainUser2.getEmail());
        assertEquals(UserState.INACTIVE, returnedAgainUser2.getState());

        assertEquals("newTest", updatedUser.getName());
        assertEquals("newTest@test.com", updatedUser.getEmail());
        assertEquals(UserState.INACTIVE, updatedUser.getState());
    }

    @Test
    @SneakyThrows
    void testUpdateFailed(BalancedDBShardingBundle<TestConfig> bundle) {
        final LookupDao<StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).update(any(String.class), any());
        try {
            val result = userStore.update("test_user_id", myUser -> new UserSummary(
                    myUser.getId(),
                    myUser.getType(),
                    "newTest@test.com",
                    "newTest",
                    UserState.INACTIVE,
                    myUser.getCreated(),
                    new Date())
            ).orElse(null);
            fail("Should have thrown exception.");
        }
        catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_UPDATE_ERROR, ((ConductorException) e).getErrorCode());
        }
    }

    @Test
    @SneakyThrows
    void testGetByIdsFailed(BalancedDBShardingBundle<TestConfig> bundle) {
        final LookupDao<StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(IllegalStateException.class).when(userDao).get(anyList());
        try {
            List<String> idList = new ArrayList<>();
            val length = 5;
            IntStream.range(0, length).forEach(i -> idList.add("userId" + i));
            userStore.getByIds(idList);
            fail("Should have thrown exception.");
        }
        catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_LIST_ERROR, ((ConductorException) e).getErrorCode());
        }
    }

    @Test
    @SneakyThrows
    void testGetByIdFailed(BalancedDBShardingBundle<TestConfig> bundle) {
        final LookupDao<StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).get(any(String.class));
        try {
            userStore.getById("testUserId");
            fail("Should have thrown exception.");
        }
        catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            System.out.println("MESSAGE: " + e.getMessage());
            assertEquals(ConductorErrorCode.STORE_READ_ERROR, ((ConductorException) e).getErrorCode());
        }
    }

    @Test
    @SneakyThrows
    void testCreateFailure(BalancedDBShardingBundle<TestConfig> bundle) {
        final LookupDao<StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).save(any(StoredUser.class));

        try {
            userStore.create("test", "Test", UserType.HUMAN, "test@test.com");
            fail("Should have thrown exception");
        }
        catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException) e).getErrorCode());
        }
    }


    private LookupDao<StoredUser> createRealUserDao(BalancedDBShardingBundle<TestConfig> bundle) {
        return bundle.createParentObjectDao(StoredUser.class);
    }

    @SuppressWarnings("unchecked")
    private LookupDao<StoredUser> createMockUserDao() {
        return (LookupDao<StoredUser>) mock(LookupDao.class);
    }
}
