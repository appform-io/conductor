package io.appform.conductor.server.actionmanagement.executors;

import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.AddTicketAction;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Executes a {@link AddTicketAction}
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AddTicketActionExecutor {

    private final TicketStore ticketStore;
    private final ActionStore actionStore;

    public ActionExecutionResult run(AddTicketAction addTicketAction,
                                     ActionExecutor.ActionEvalData evalData) {
        val ticketId = evalData.getTicket().getSummary().getId();
        val actionId = addTicketAction.getActionId();
        val actionOptional = actionStore.read(actionId);
        if (actionOptional.isPresent()
                && ticketStore.addAction(ticketId, actionId)) {
            return ActionExecutionResult.SUCCESS;
        }
        log.error("Failed to add action to ticket {}", ticketId);
        return ActionExecutionResult.FAILURE;
    }
}
