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

package io.appform.conductor.usermgmt.store.impl;

import io.appform.conductor.DBTestBase;
import io.appform.conductor.error.ConductorErrorCode;
import io.appform.conductor.error.ConductorException;
import io.appform.conductor.usermgmt.model.UserState;
import io.appform.conductor.usermgmt.store.UserStore;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DBUserStore}
 */
class DBUserStoreTest extends DBTestBase {

    @Test
    public void testCreate() {
        val userStore = new DBUserStore(createRealUserDao());
        val createdUser = userStore.create("Test", "test@test.com", "TestPassword")
                .orElse(null);
        assertNotNull(createdUser);
        assertEquals("Test", createdUser.getName());
        assertEquals("test@test.com", createdUser.getEmail());
        assertEquals("TestPassword", createdUser.getPassword());
    }

    @Test
    public void testGetById() {
        val userStore = new DBUserStore(createRealUserDao());
        val newUser = userStore.create("Test", "test@test.com", "testpassword")
                .orElse(null);
        assertNotNull(newUser);
        val newId = newUser.getId();
        val returnedUser = userStore.getById(newId).orElse(null);
        assertNotNull(returnedUser);
        assertEquals(newUser, returnedUser);
    }

    @Test
    public void testGetByIds() {
        val numOfUsers = 10;
        val userStore = new DBUserStore(createRealUserDao());
        List<String> queryIds = new ArrayList<>();
        val users = new ArrayList<UserStore.UserDetails>();
        IntStream.range(0, numOfUsers).forEach(
                i -> {
                    val newUser = userStore.create("Test"+i, "test"+i+"@gmail.com", "testpassword"+i)
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
                i-> assertTrue(returnedUsers.contains(users.get(i)))
        );
    }

    @Test
    public void testGetByEmailFailed() {
        //User not found
        val userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        val emptyList = new ArrayList<UserStore.UserDetails>();
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
            assertTrue( e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_READ_ERROR, ((ConductorException)e).getErrorCode());
        }
    }
  
    @Test
    public void testGetByEmail() {
        val userStore = new DBUserStore(createRealUserDao());
        val newUser = userStore.create("Test", "test@test.com", "testpassword")
                .orElse(null);
        assertNotNull(newUser);
        val newEmail = newUser.getEmail();
        val returnedUser = userStore.getByEmail(newEmail).orElse(null);
        assertNotNull(returnedUser);
        assertEquals(newUser, returnedUser);
    }

    @Test
    public void testUpdate() {
        val userStore = new DBUserStore(createRealUserDao());
        val newUser1 = userStore.create("Test1", "test1@test.com", "testPassword1").orElse(null);
        val newUser2 =userStore.create("Test2", "test2@test.com", "testPassword2").orElse(null);
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
        assertEquals("testPassword1", returnedUser1.getPassword());

        assertEquals("Test2", returnedUser2.getName());
        assertEquals("test2@test.com", returnedUser2.getEmail());
        assertEquals("testPassword2", returnedUser2.getPassword());
        val updatedUser = userStore.update(newId2, myUser -> {
            myUser.setEmail("newTest@test.com");
            myUser.setName("newTest");
            myUser.setPassword("newTestPassword");
            myUser.setFailedPasswordAttempts(newUser2.getFailedPasswordAttempts() + 1);
            myUser.setState(UserState.INACTIVE);
        }).orElse(null);
        assertNotNull(updatedUser);
        val returnedAgainUser1 = userStore.getById(newId1).orElse(null);
        val returnedAgainUser2 = userStore.getById(newId2).orElse(null);
        assertNotNull(returnedAgainUser1);
        assertNotNull(returnedAgainUser2);

        assertEquals("Test1", returnedAgainUser1.getName());
        assertEquals("test1@test.com", returnedAgainUser1.getEmail());
        assertEquals("testPassword1", returnedAgainUser1.getPassword());

        assertEquals("newTest", returnedAgainUser2.getName());
        assertEquals("newTest@test.com", returnedAgainUser2.getEmail());
        assertEquals("newTestPassword", returnedAgainUser2.getPassword());
        assertEquals(newUser2.getFailedPasswordAttempts() + 1, returnedAgainUser2.getFailedPasswordAttempts());
        assertEquals(UserState.INACTIVE, returnedAgainUser2.getState());

        assertEquals("newTest", updatedUser.getName());
        assertEquals("newTest@test.com", updatedUser.getEmail());
        assertEquals("newTestPassword", updatedUser.getPassword());
        assertEquals(newUser2.getFailedPasswordAttempts() + 1, updatedUser.getFailedPasswordAttempts());
        assertEquals(UserState.INACTIVE, updatedUser.getState());
    }

    @Test
    @SneakyThrows
    public void testUpdateFailed() {
        final LookupDao<DBUserStore.StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).update(any(String.class), any());
        try {
            val result = userStore.update("test_user_id", updateUser -> {
                updateUser.setEmail("newemail@test.com");
                updateUser.setName("newTest");
            }).orElse(null);
            fail("Should have thrown exception.");
        }
        catch (Exception e) {
            assertTrue( e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_UPDATE_ERROR, ((ConductorException)e).getErrorCode());
        }
    }

    @Test
    @SneakyThrows
    public void testGetByIdsFailed() {
        final LookupDao<DBUserStore.StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).get(anyList());
        try{
            List<String> idList = new ArrayList<>();
            val length = 5;
            IntStream.range(0, length).forEach(i ->
                    idList.add("userId" + i));
            userStore.getByIds(idList);
            fail("Should have thrown exception.");
        }
        catch (Exception e) {
            assertTrue( e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_LIST_ERROR, ((ConductorException)e).getErrorCode());
        }
    }

    @Test
    @SneakyThrows
    public void testGetByIdFailed() {
        final LookupDao<DBUserStore.StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).get(any(String.class));
        try{
            userStore.getById("testUserId");
            fail("Should have thrown exception.");
        }
        catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_READ_ERROR, ((ConductorException)e).getErrorCode());
        }
    }

    @Test
    @SneakyThrows
    public void testCreateFailure() {
        final LookupDao<DBUserStore.StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).save(any(DBUserStore.StoredUser.class));

        try {
            userStore.create("Test", "test@test.com", "TestPassword");
            fail("Should have thrown exception");
        }
        catch (Exception e) {
            assertTrue( e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException)e).getErrorCode());
        }
    }


    private LookupDao<DBUserStore.StoredUser> createRealUserDao() {
        return bundle.createParentObjectDao(DBUserStore.StoredUser.class);
    }

    @SuppressWarnings("unchecked")
    private LookupDao<DBUserStore.StoredUser> createMockUserDao() {
        return (LookupDao<DBUserStore.StoredUser>)mock(LookupDao.class);
    }
}
