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

package io.appform.conductor.core.actionmanagement.executors;

import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.RouteToGroupAction;
import io.appform.conductor.core.actionmanagement.ActionExecutor;
import io.appform.conductor.core.ticketmanagement.TicketStore;
import io.appform.conductor.core.interfaces.GroupStore;
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
public class RouteToGroupActionExecutor {
    private final GroupStore groupStore;
    private final TicketStore ticketStore;

    public ActionExecutionResult run(RouteToGroupAction action, final ActionExecutor.ActionEvalData evalData) {
        val group = groupStore.read(action.getGroupId()).filter(g -> !g.isDeleted()).orElse(null);
        val ticketId = evalData.getTicket().getSummary().getId();
        if(null == group) {
            log.info("Could not assign ticket {} to non-existent group {}",
                     ticketId,
                     action.getGroupId());
            return ActionExecutionResult.FAILURE;
        }
        if(ticketStore.assignToGroup(ticketId, action.getGroupId(), null)
                .filter(ticket -> ticket.getAssignedToGroupId().equals(action.getGroupId()))
                .isPresent()) {
            return ActionExecutionResult.SUCCESS;
        }
        log.error("Failed to route ticket {} to group {}", ticketId, group.getName());
        return ActionExecutionResult.FAILURE;
    }
}
