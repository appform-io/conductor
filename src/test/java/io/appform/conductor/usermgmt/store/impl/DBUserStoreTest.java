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
import io.appform.conductor.usermgmt.store.UserStore;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link DBUserStore}
 */
public class DBUserStoreTest extends DBTestBase {

    @Test
    public void testCreate() {
        val userStore = new DBUserStore(createRealUserDao());
        val createdUser = userStore.create("Test", "test@test.com", "TestPassword")
                .orElse(null);
        Assert.assertNotNull(createdUser);
        Assert.assertEquals("Test", createdUser.getName());
        Assert.assertEquals("test@test.com", createdUser.getEmail());
        Assert.assertEquals("TestPassword", createdUser.getPassword());
    }

    @Test
    public void testGetById() {
        val userStore = new DBUserStore(createRealUserDao());
        val newUser = userStore.create("Test", "test@test.com", "testpassword")
                .orElse(null);
        Assert.assertNotNull(newUser);
        val newId = newUser.getId();
        val returnedUser = userStore.getById(newId).orElse(null);
        Assert.assertNotNull(returnedUser);
        Assert.assertEquals(newUser, returnedUser);
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
                    Assert.assertNotNull(newUser);
                    val newId = newUser.getId();
                    users.add(newUser);
                    queryIds.add(newId);
                }
        );

        val returnedUsers = userStore.getByIds(queryIds);
        Assert.assertEquals(numOfUsers, queryIds.size());
        Assert.assertEquals(numOfUsers, returnedUsers.size());
        IntStream.range(0, numOfUsers).forEach(
                i-> Assert.assertTrue(returnedUsers.contains(users.get(i)))
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
        Assert.assertNull(user);

        //scatterGather throws exception
        val userDao2 = createMockUserDao();
        val userStore2 = new DBUserStore(userDao2);
        doThrow(NullPointerException.class).when(userDao2).scatterGather(any(DetachedCriteria.class));
        try {
            userStore2.getByEmail("testMail");
            Assert.fail("Should have thrown exception.");
        }
        catch (Exception e) {
            Assert.assertTrue( e instanceof ConductorException);
            Assert.assertEquals(ConductorErrorCode.STORE_READ_ERROR, ((ConductorException)e).getErrorCode());
        }
    }

    @Test
    @SneakyThrows
    public void testGetByIdsFailed() {
        final LookupDao<DBUserStore.StoredUser> userDao = createMockUserDao();
        val userStore = new DBUserStore(userDao);
        doThrow(NullPointerException.class).when(userDao).get(anyListOf(String.class));
        try{
            List<String> idList = new ArrayList<>();
            val length = 5;
            IntStream.range(0, length).forEach(i ->
                    idList.add("userId" + i));
            userStore.getByIds(idList);
            Assert.fail("Should have thrown exception.");
        }
        catch (Exception e){
            Assert.assertTrue( e instanceof ConductorException);
            Assert.assertEquals(ConductorErrorCode.STORE_LIST_ERROR, ((ConductorException)e).getErrorCode());
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
            Assert.fail("Should have thrown exception.");
        }
        catch (Exception e){
            Assert.assertTrue( e instanceof ConductorException);
            Assert.assertEquals(ConductorErrorCode.STORE_READ_ERROR, ((ConductorException)e).getErrorCode());
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
            Assert.fail("Should have thrown exception");
        }
        catch (Exception e) {
            Assert.assertTrue( e instanceof ConductorException);
            Assert.assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException)e).getErrorCode());
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
