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

import com.google.inject.Inject;
import io.appform.conductor.DBTestBase;
import io.appform.conductor.error.ConductorErrorCode;
import io.appform.conductor.error.ConductorException;
import io.appform.conductor.usermgmt.store.UserStore;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link DBUserStore}
 */
public class DBUserStoreTest extends DBTestBase {

    @Inject
    private LookupDao<UserStore> userStore;



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
    @SneakyThrows
    public void testCreateFailure() {
        final LookupDao<DBUserStore.StoredUser> userDao = createMockserDao();
        val userStore = new DBUserStore(userDao);
        when(userDao.save(any(DBUserStore.StoredUser.class))).thenThrow(NullPointerException.class);

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

    private LookupDao<DBUserStore.StoredUser> createMockserDao() {
        return (LookupDao<DBUserStore.StoredUser>)mock(LookupDao.class);
    }
}
