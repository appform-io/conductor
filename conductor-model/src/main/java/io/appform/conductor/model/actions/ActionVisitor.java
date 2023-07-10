/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.model.actions;

import io.appform.conductor.model.actions.impl.*;

/**
 * This needs to be implemented to handle {@link Action} subtypes specific action.
 * For example execution, validation, rendering etc
 */
public interface ActionVisitor<T> {

    T visit(WebhookAction webhookAction);

    T visit(RouteToGroupAction routeToGroupAction);

    T visit(AddCommentAction addCommentAction);

    T visit(AddTicketAction addTicketAction);

    T visit(ChangePriorityAction changePriorityAction);

    T visit(SetFieldAction setFieldAction);
}
