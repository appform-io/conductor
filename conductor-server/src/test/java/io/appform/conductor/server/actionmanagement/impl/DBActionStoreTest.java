package io.appform.conductor.server.actionmanagement.impl;

import io.appform.conductor.model.actions.ActionScope;
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


import java.util.Map;
import java.util.Set;

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
        val createdAction = actionStore.save(AddTicketAction.builder()
                        .actionId("NewAddTicketAction")
                        .id("TestAddTicketAction1")
                        .name("TestingNameAddTicketAction")
                        .description("Testing description for AddTicketAction")
                        .scope(ActionScope.build(ActionScope.ScopeType.STATE, "S123"))
                        .build())
                .orElse(null);
        assertNotNull(createdAction);
        assertEquals("TestAddTicketAction1", createdAction.getId());
        assertNotNull(createdAction.getCreated());
    }

    @Test
    void testRead(BalancedDBShardingBundle<TestConfig> bundle) {
        val actionStore = new DBActionStore(createRealActionLookupDao(bundle));
        val createdAction = actionStore.save(AddCommentAction.builder()
                        .contentTemplate(new Template(Template.Type.HANDLEBARS, "CommentTemplate"))
                        .id("TestAddCommentAction1")
                        .name("TestingNameAddCommentAction")
                        .scope(ActionScope.build(ActionScope.ScopeType.STATE, "S123"))
                        .description("Testing description for AddCommentAction")
                        .build())
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
    void testCreateAndReadWebhookAction(BalancedDBShardingBundle<TestConfig> bundle) {
        val actionStore = new DBActionStore(createRealActionLookupDao(bundle));
        val createdAction = actionStore.save(WebhookAction.builder()
                        .id("TestWebhookAction")
                        .name("TestingNameWebhookAction")
                        .description("Testing description for WebhookAction")
                        .scope(ActionScope.build(ActionScope.ScopeType.STATE, "S123"))
                        .callMode(WebhookAction.CallMode.SYNC)
                        .callType(WebhookAction.CallType.POST)
                        .urlTemplate(new Template(Template.Type.STRING_SUBSTITUTION,"urlTemplate"))
                        .headerTemplates(Map.of("headerName1", new Template(Template.Type.STRING_SUBSTITUTION,"headersTemplate")))
                        .payloadTemplate(new Template(Template.Type.STRING_SUBSTITUTION,"payloadTemplate"))
                        .mimeType(WebhookAction.MimeType.JSON)
                        .successCodes(Set.of(200,202))
                        .timeoutMs(2000)
                        .retryStrategy(WebhookAction.RetryStrategy.FIXED_INTERVAL)
                        .numRetries(3)
                        .build())
                .orElse(null);
        assertNotNull(createdAction);
        assertEquals("TestWebhookAction", createdAction.getId());
        assertNotNull(createdAction.getCreated());
        assertEquals(1, ((WebhookAction)createdAction).getHeaderTemplates().size());

        val readAction = actionStore.read(createdAction.getId())
                .orElse(null);
        assertNotNull(readAction);
        assertEquals(createdAction.getId(), readAction.getId());
        assertNotNull(readAction.getCreated());
        assertEquals(1, ((WebhookAction)readAction).getHeaderTemplates().size());

    }


    @Test
    void testSaveActionFailed(BalancedDBShardingBundle<TestConfig> bundle) throws Exception {
        LookupDao<StoredAction> actionLookupDao = createMockActionLookupDao();
        val actionStore = new DBActionStore(actionLookupDao);
        doThrow(NullPointerException.class).when(actionLookupDao).save(any());
        try {
            actionStore.save(AddTicketAction.builder()
                    .actionId("NewTicketActionId")
                    .id("TestNormalActionId1")
                    .name("TestingName")
                    .description("Testing description")
                    .scope(ActionScope.build(ActionScope.ScopeType.STATE, "S123"))
                    .build());
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
