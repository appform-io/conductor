package io.appform.conductor.usermgmt.store.impl;

import io.appform.conductor.DBTestBase;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.server.store.impl.DBSessionStore;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DBSessionStore}
 */
class DBSessionStoreTest extends DBTestBase {

    @Test
    void testCreate() {
        val userStore = new DBSessionStore(createRealUserDao());
        val newUserSession = userStore.create("user_id").orElse(null);
        assertNotNull(newUserSession);
        assertEquals("user_id", newUserSession.getUserId());
        assertEquals(SessionState.ACTIVE, newUserSession.getState());
    }

    @Test
    @SneakyThrows
    void testCreateFailed() {
        val userDao = createMockUserDao();
        val mockUserDao = new DBSessionStore(userDao);
        doThrow(NullPointerException.class).when(userDao).save(anyString(), any(DBSessionStore.StoredUserSessionDetails.class));
        try {
            mockUserDao.create("user_id");
            fail("Should have thrown exception");
        }
        catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException) e).getErrorCode());
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