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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.conductor.model.actions.ActionExecutionResult;
import io.appform.conductor.model.actions.impl.AddCommentAction;
import io.appform.conductor.core.actionmanagement.ActionExecutor;
import io.appform.conductor.core.templateengines.TemplateEngine;
import io.appform.conductor.core.ticketmanagement.TicketStore;
import io.appform.conductor.core.utils.ConductorServerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

/**
 * Executes a {@link AddCommentAction}
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AddCommentActionExecutor {
    private final TicketStore ticketStore;
    private final TemplateEngine templateEngine;
    private final ObjectMapper mapper;

    public ActionExecutionResult run(AddCommentAction action, final ActionExecutor.ActionEvalData evalData) {
        val ticketId = evalData.getTicket().getSummary().getId();
        val comment = templateEngine.evaluateToText(action.getContentTemplate(),
                                                          ConductorServerUtils.evalDataJson(mapper,
                                                                  evalData)).orElse(null);
        if(!Strings.isNullOrEmpty(comment)
        && ticketStore.addComment(ticketId, ConductorServerUtils.generateCommentId(), comment, null).isPresent()) {
            return ActionExecutionResult.SUCCESS;
        }
        log.error("Failed to add comment to ticket {}", ticketId);
        return ActionExecutionResult.FAILURE;
    }
}
