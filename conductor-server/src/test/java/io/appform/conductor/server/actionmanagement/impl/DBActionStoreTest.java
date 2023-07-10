package io.appform.conductor.server.actionmanagement.impl;

import io.appform.conductor.model.actions.impl.*;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.actionmanagement.impl.models.StoredAction;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DBActionStore}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.actionmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBActionStoreTest {

    @Test
    void testCreate(BalancedDBShardingBundle<TestConfig> bundle) {
        val actionStore = new DBActionStore(createRealActionLookupDao(bundle));
        val createdAction = actionStore.save(new AddTicketAction()
                        .setActionId("NewAddTicketAction")
                        .setId("TestAddTicketAction1")
                        .setName("TestingNameAddTicketAction")
                        .setDescription("Testing description for AddTicketAction"))
                .orElse(null);
        assertNotNull(createdAction);
        assertEquals("TestAddTicketAction1", createdAction.getId());
        assertNotNull(createdAction.getCreated());
    }

    @Test
    void testRead(BalancedDBShardingBundle<TestConfig> bundle) {
        val actionStore = new DBActionStore(createRealActionLookupDao(bundle));
        val createdAction = actionStore.save(new AddCommentAction()
                        .setContentTemplate(new Template(Template.Type.HANDLEBARS, "CommentTemplate"))
                        .setId("TestAddCommentAction1")
                        .setName("TestingNameAddCommentAction")
                        .setDescription("Testing description for AddCommentAction"))
                .orElse(null);
        assertNotNull(createdAction);
        assertEquals("TestAddCommentAction1", createdAction.getId());
        assertNotNull(createdAction.getCreated());

        val readAction = actionStore.read(createdAction.getId())
                .orElse(null);
        ;
        assertNotNull(readAction);
        assertEquals(createdAction.getId(), readAction.getId());
        assertNotNull(readAction.getCreated());
    }


    @Test
    void testSaveActionFailed(BalancedDBShardingBundle<TestConfig> bundle) throws Exception {
        LookupDao<StoredAction> actionLookupDao = createMockActionLookupDao();
        val actionStore = new DBActionStore(actionLookupDao);
        doThrow(NullPointerException.class).when(actionLookupDao).save(any());
        try {
            actionStore.save(new AddTicketAction()
                    .setActionId("NewTicketActionId")
                    .setId("TestNormalActionId1")
                    .setName("TestingName")
                    .setDescription("Testing description"));
            fail("Should have thrown exception.");
        } catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException) e).getErrorCode());
        }

    }

    @Test
    void testReadActionFailed(BalancedDBShardingBundle<TestConfig> bundle) throws Exception {
        LookupDao<StoredAction> actionLookupDao = createMockActionLookupDao();
        val actionStore = new DBActionStore(actionLookupDao);
        doThrow(NullPointerException.class).when(actionLookupDao).get(anyString());
        try {
            actionStore.read("actionId1");
            fail("Should have thrown exception.");
        } catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_READ_ERROR, ((ConductorException) e).getErrorCode());
        }

    }

    private LookupDao<StoredAction> createRealActionLookupDao(BalancedDBShardingBundle<TestConfig> bundle) {
        return bundle.createParentObjectDao(StoredAction.class);
    }

    @SuppressWarnings("unchecked")
    private LookupDao<StoredAction> createMockActionLookupDao() {
        return (LookupDao<StoredAction>) mock(LookupDao.class);
    }
}
