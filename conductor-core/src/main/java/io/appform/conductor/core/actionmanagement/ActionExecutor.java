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

package io.appform.conductor.server.actionmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.ActionVisitor;
import io.appform.conductor.model.actions.impl.*;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.server.actionmanagement.executors.*;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Executes an action
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ActionExecutor {

    private final RouteToGroupActionExecutor routeToGroupActionExecutor;
    private final ChangePriorityActionExecutor changePriorityActionExecutor;
    private final SetFieldActionExecutor setFieldActionExecutor;
    private final AddCommentActionExecutor addCommentActionExecutor;
    private final WebhookActionExecutor webhookActionExecutor;
    private final AddTicketActionActionExecutor addTicketActionActionExecutor;

    @Value
    public static class ActionEvalData {
        Workflow workflow;
        Schema schema;
        TicketDetails ticket;
        JsonNode payload;
        User requester;
    }

    public ActionExecutionResult execute(final Action action, final ActionEvalData evalData) {
        return action.accept(new ActionVisitor<>() {
            @Override
            public ActionExecutionResult visit(WebhookAction webhookAction) {
                return webhookActionExecutor.run(webhookAction, evalData);
            }

            @Override
            public ActionExecutionResult visit(RouteToGroupAction routeToGroupAction) {
                return routeToGroupActionExecutor.run(routeToGroupAction, evalData);
            }

            @Override
            public ActionExecutionResult visit(AddCommentAction addCommentAction) {
                return addCommentActionExecutor.run(addCommentAction, evalData);
            }

            @Override
            public ActionExecutionResult visit(AddTicketAction addTicketAction) {
                return addTicketActionActionExecutor.run(addTicketAction, evalData);
            }

            @Override
            public ActionExecutionResult visit(ChangePriorityAction changePriorityAction) {
                return changePriorityActionExecutor.run(changePriorityAction, evalData);
            }

            @Override
            public ActionExecutionResult visit(SetFieldAction setFieldAction) {
                return setFieldActionExecutor.run(setFieldAction, evalData);
            }
        });
    }
}
