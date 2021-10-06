package io.appform.conductor.usermgmt.store.impl;

import io.appform.conductor.DBTestBase;
import io.appform.conductor.error.ConductorErrorCode;
import io.appform.conductor.error.ConductorException;
import io.appform.conductor.usermgmt.store.SessionStore;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link DBSessionStore}
 */
public class DBSessionStoreTest extends DBTestBase {

    @Test
    public void testCreate() {
        val userStore = new DBSessionStore(createRealUserDao());
        val newUserSession = userStore.create("user_id").orElse(null);
        Assert.assertNotNull(newUserSession);
        Assert.assertEquals("user_id", newUserSession.getUserId());
        Assert.assertEquals(SessionStore.SessionState.ACTIVE, newUserSession.getState());
    }

    @Test
    @SneakyThrows
    public void testCreateFailed() {
        val userDao = createMockUserDao();
        val mockUserDao = new DBSessionStore(userDao);
        doThrow(NullPointerException.class).when(userDao).save(anyString(), any(DBSessionStore.StoredUserSessionDetails.class));
        try {
            mockUserDao.create("user_id");
            Assert.fail("Should have thrown exception");
        }
        catch (Exception e) {
            Assert.assertTrue(e instanceof ConductorException);
            Assert.assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException) e).getErrorCode());
        }
    }

    private RelationalDao<DBSessionStore.StoredUserSessionDetails> createRealUserDao() {
        return  bundle.createRelatedObjectDao(DBSessionStore.StoredUserSessionDetails.class);
    }

    @SuppressWarnings("unchecked")
    private RelationalDao<DBSessionStore.StoredUserSessionDetails> createMockUserDao() {
        return  (RelationalDao<DBSessionStore.StoredUserSessionDetails>)mock(RelationalDao.class);
    }
}