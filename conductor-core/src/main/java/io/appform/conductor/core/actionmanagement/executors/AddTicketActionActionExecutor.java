/*
 * Copyright (c) 2023 Santanu Sinha
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

package io.appform.conductor.server.actionmanagement.executors;

import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.AddTicketAction;
import io.appform.conductor.model.actions.impl.RouteToGroupAction;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Executes a {@link RouteToGroupAction}
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AddTicketActionActionExecutor {
    private final ActionStore actionStore;
    private final TicketStore ticketStore;

    public ActionExecutionResult run(AddTicketAction addTicketAction, final ActionExecutor.ActionEvalData evalData) {
        val action = actionStore.read(addTicketAction.getActionId()).orElse(null);
        val ticketId = evalData.getTicket().getSummary().getId();
        if(null == action) {
            log.info("Could not add non-existent action {} to ticket {}",
                     addTicketAction.getActionId(), ticketId);
            return ActionExecutionResult.FAILURE;
        }
        if(ticketStore.addTicketAction(ticketId, addTicketAction.getActionId())
                .filter(ticket -> ticket.getTicketActionsIds().contains(addTicketAction.getActionId()))
                .isPresent()) {
            return ActionExecutionResult.SUCCESS;
        }
        log.error("Failed to route ticket {} to action {}", ticketId, action.getName());
        return ActionExecutionResult.FAILURE;
    }
}
