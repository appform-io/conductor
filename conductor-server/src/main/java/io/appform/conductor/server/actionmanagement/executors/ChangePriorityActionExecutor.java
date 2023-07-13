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
import io.appform.conductor.model.actions.impl.ChangePriorityAction;
import io.appform.conductor.server.actionmanagement.ActionExecutor;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Executes a {@link ChangePriorityAction}
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ChangePriorityActionExecutor {
    private final TicketStore ticketStore;

    public ActionExecutionResult run(ChangePriorityAction action, final ActionExecutor.ActionEvalData evalData) {
        val ticketId = evalData.getTicket().getSummary().getId();
        if(ticketStore.update(ticketId,
                              ticket -> ticket.setPriority(action.getPriority()),
                              List.of()).isPresent()) {
            return ActionExecutionResult.SUCCESS;
        }
        log.error("Failed to change priority for ticket {} to {}", ticketId, action.getPriority());
        return ActionExecutionResult.FAILURE;
    }
}
