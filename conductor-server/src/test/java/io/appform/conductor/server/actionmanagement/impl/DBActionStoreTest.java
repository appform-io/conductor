package io.appform.conductor.server.actionmanagement.impl;

import io.appform.conductor.model.actions.ActionErrorHandlingStrategy;
import io.appform.conductor.model.actions.impl.*;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.fields.impl.StringFieldValue;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.actionmanagement.impl.models.StoredAction;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
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
        val actionStore = new DBActionStore(createRealActionRelationalDao(bundle));
        val createdAction = actionStore.save(AddTicketAction.builder()
                        .id("TestAddTicketAction1")
                        .name("TestingNameAddTicketAction")
                        .description("Testing description for AddTicketAction")
                        .actionId("NewAddTicketAction")
                        .build())
                .orElse(null);
        assertNotNull(createdAction);
        assertEquals("TestAddTicketAction1", createdAction.getId());
        assertNotNull(createdAction.getCreated());
    }

    @Test
    void testRead(BalancedDBShardingBundle<TestConfig> bundle) {
        val actionStore = new DBActionStore(createRealActionRelationalDao(bundle));
        val createdAction = actionStore.save(AddCommentAction.builder()
                        .id("TestAddCommentAction1")
                        .name("TestingNameAddCommentAction")
                        .description("Testing description for AddCommentAction")
                        .contentTemplate(new Template(Template.Type.HANDLEBARS, "CommentTemplate"))
                        .build())
                .orElse(null);
        assertNotNull(createdAction);
        assertEquals("TestAddCommentAction1", createdAction.getId());
        assertNotNull(createdAction.getCreated());

        val readAction = actionStore.read(createdAction.getId())
                .orElse(null);;
        assertNotNull(readAction);
        assertEquals(createdAction.getId(), readAction.getId());
        assertNotNull(readAction.getCreated());
    }

    @Test
    void testCreateAndReadCompositeAction(BalancedDBShardingBundle<TestConfig> bundle) {
        val actionStore = new DBActionStore(createRealActionRelationalDao(bundle));
        val compositeAction = CompositionAction.builder()
                .id("TestCompositionAction1")
                .name("TestingNameCompositionAction")
                .description("Testing description for CompositionAction")
                .errorHandlingStrategy(ActionErrorHandlingStrategy.IGNORE)
                .children(List.of(
                        AddTicketAction.builder()
                                .id("TestAddTicketAction")
                                .name("TestingNameAddTicketAction")
                                .description("Testing description for AddTicketAction")
                                .actionId("NewAddTicketAction")
                                .build(),
                        AddCommentAction.builder()
                                .id("TestAddCommentAction")
                                .name("TestingNameAddCommentAction")
                                .description("Testing description for AddCommentAction")
                                .contentTemplate(new Template(Template.Type.HANDLEBARS, "CommentTemplate"))
                                .build(),
                        ChangePriorityAction.builder()
                                .id("TestChangePriorityAction")
                                .name("TestingNameChangePriorityAction")
                                .description("Testing description for ChangePriorityAction")
                                .priority(TicketPriority.HIGH)
                                .build(),
                        RouteToGroupAction.builder()
                                .id("TestRouteToGroupAction")
                                .name("TestingNameRouteToGroupAction")
                                .description("Testing description for RouteToGroupAction")
                                .groupId("groupId")
                                .build(),
                        SetFieldAction.builder()
                                .id("TestSetFieldAction")
                                .name("TestingNameSetFieldAction")
                                .description("Testing description for SetFieldAction")
                                .fieldSchemaId("fieldSchemaId")
                                .fieldValue(new StringFieldValue("StringValue"))
                                .build(),
                        WebhookAction.builder()
                                .id("TestWebhookAction")
                                .name("TestingNameWebhookAction")
                                .description("Testing description for WebhookAction")
                                .callMode(WebhookAction.CallMode.SYNC)
                                .callType(WebhookAction.CallType.POST)
                                .urlTemplate(new Template(Template.Type.STRING_SUBSTITUTION,"urlTemplate"))
                                .headersTemplate(new Template(Template.Type.STRING_SUBSTITUTION,"headersTemplate"))
                                .payloadTemplate(new Template(Template.Type.STRING_SUBSTITUTION,"payloadTemplate"))
                                .mimeType("application/pdf")
                                .successCodes(Set.of(200,202))
                                .timeoutMs(2000)
                                .retryStrategy(WebhookAction.RetryStrategy.FIXED_INTERVAL)
                                .numRetries(3)
                                .build()
                ))
                .build();

        val createdAction = actionStore.save(compositeAction)
                .orElse(null);
        assertNotNull(createdAction);
        assertEquals(compositeAction.getId(), createdAction.getId());
        assertEquals(6, ((CompositionAction)createdAction).getChildren().size());
        assertNotNull(createdAction.getCreated());

        val readAction = actionStore.read(compositeAction.getId())
                .orElse(null);;
        assertNotNull(readAction);
        assertEquals(compositeAction.getId(), readAction.getId());
        assertEquals(6, ((CompositionAction)readAction).getChildren().size());
        assertNotNull(readAction.getCreated());

    }


    @Test
    void testSaveActionFailed(BalancedDBShardingBundle<TestConfig> bundle) throws Exception {
        RelationalDao<StoredAction> actionRelationalDao = createMockActionRelationalDao();
        val actionStore = new DBActionStore(actionRelationalDao);
        doThrow(NullPointerException.class).when(actionRelationalDao).save(anyString(), any());
        try {
            actionStore.save(AddTicketAction.builder()
                    .id("TestNormalActionId1")
                    .name("TestingName")
                    .description("Testing description")
                    .actionId("NewTicketActionId")
                    .build());
            fail("Should have thrown exception.");
        } catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_WRITE_ERROR, ((ConductorException) e).getErrorCode());
        }

    }

    @Test
    void testReadActionFailed(BalancedDBShardingBundle<TestConfig> bundle) throws Exception {
        RelationalDao<StoredAction> actionRelationalDao = createMockActionRelationalDao();
        val actionStore = new DBActionStore(actionRelationalDao);
        doThrow(NullPointerException.class).when(actionRelationalDao).select(anyString(), any(), anyInt(), anyInt());
        try {
            actionStore.read("actionId1");
            fail("Should have thrown exception.");
        } catch (Exception e) {
            assertTrue(e instanceof ConductorException);
            assertEquals(ConductorErrorCode.STORE_READ_ERROR, ((ConductorException) e).getErrorCode());
        }

    }

    private RelationalDao<StoredAction> createRealActionRelationalDao(BalancedDBShardingBundle<TestConfig> bundle) {
        return bundle.createRelatedObjectDao(StoredAction.class);
    }

    @SuppressWarnings("unchecked")
    private RelationalDao<StoredAction> createMockActionRelationalDao() {
        return (RelationalDao<StoredAction>) mock(RelationalDao.class);
    }
}
