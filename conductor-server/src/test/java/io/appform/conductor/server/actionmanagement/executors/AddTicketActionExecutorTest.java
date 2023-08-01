package io.appform.conductor.server.actionmanagement.executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.AddCommentAction;
import io.appform.conductor.model.actions.impl.AddTicketAction;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.TicketSummary;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.actionmanagement.impl.DBActionStore;
import io.appform.conductor.server.actionmanagement.impl.models.StoredAction;
import io.appform.conductor.server.ticketmanagement.impl.DBTicketStore;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.actions.StoredTicketAction;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.dropwizard.jackson.Jackson;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.appform.conductor.server.utils.ConductorServerUtils.configureMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RelevantDBEntityPackages({"io.appform.conductor.server.actionmanagement.impl.models", "io.appform.conductor.server.ticketmanagement.impl.models"})
@ExtendWith(DBTestExtension.class)
public class AddTicketActionExecutorTest {

    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

    @BeforeAll
    static void setup() {
        configureMapper(MAPPER);
    }

    @Test
    @SneakyThrows
    void testAddTicketActionExecutorSuccess(BalancedDBShardingBundle<TestConfig> bundle) {
        val actionStore = new DBActionStore(bundle.createParentObjectDao(StoredAction.class));
        val ticketStore = new DBTicketStore(bundle.createParentObjectDao(StoredTicketSkeleton.class),
                bundle.createRelatedObjectDao(StoredFieldValue.class),
                bundle.createRelatedObjectDao(StoredComment.class),
                bundle.createRelatedObjectDao(StoredAttachment.class),
                bundle.createRelatedObjectDao(StoredTicketAction.class),
                MAPPER);
        val ticketId = "TestTicketId1";
        val actionIdOfToBeAddedAction = "ActionIdOfToBeAddedAction";
        val ticketSummary = new TicketSummary(ticketId, null, null, null, null, null, null, null, null, null, null, null);
        val actionEvalData = new ActionExecutor.ActionEvalData(null,null,
                new TicketDetails(ticketSummary, null, null), null, null);
        val addTicketAction = AddTicketAction.builder()
                .id("TestAddTicketAction2")
                .actionId(actionIdOfToBeAddedAction)
                .name("TestingNameAddTicketAction")
                .description("Testing description for AddTicketAction")
                .build();
        val actionToBeAdded = actionStore.save(AddCommentAction.builder()
                .contentTemplate(new Template(Template.Type.HANDLEBARS, "CommentTemplate"))
                .id(actionIdOfToBeAddedAction)
                .name("TestingNameAddCommentAction")
                .description("Testing description for AddCommentAction")
                .build()).orElse(null);
        assertNotNull(actionToBeAdded);
        val executor = new AddTicketActionExecutor(ticketStore, actionStore);
        val actionExecutionResult = executor.run(addTicketAction, actionEvalData);

        assertEquals(ActionExecutionResult.SUCCESS, actionExecutionResult);

    }
}
