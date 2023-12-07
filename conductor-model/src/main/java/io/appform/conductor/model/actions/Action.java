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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.actions.impl.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * An action that can be taken as a transition
 */
@Data
@EqualsAndHashCode(exclude = {"created", "updated"})
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = ActionType.ADD_COMMENT_TEXT, value = AddCommentAction.class),
        @JsonSubTypes.Type(name = ActionType.ADD_TICKET_ACTION_TEXT, value = AddTicketAction.class),
        @JsonSubTypes.Type(name = ActionType.CHANGE_PRIORITY_TEXT, value = ChangePriorityAction.class),
        @JsonSubTypes.Type(name = ActionType.ROUTE_TO_GROUP_TEXT, value = RouteToGroupAction.class),
        @JsonSubTypes.Type(name = ActionType.SET_FIELD_TEXT, value = SetFieldAction.class),
        @JsonSubTypes.Type(name = ActionType.WEBHOOK_TEXT, value = WebhookAction.class),
})
public abstract class Action {


    /**
     * Type of action
     */
    private final ActionType type;

    /**
     * Global ID for the action
     */
    private final String id;

    /**
     * Human-readable name for the action
     */
    private final String name;

    /**
     * Human-readable description of what the action does
     */
    private final String description;

    /**
     * Scope of the action i.e STATE, TRANSITION, GLOBAL etc along with the identifier for the same
     */
    private final Scope scope;

    /**
     * Date when action was created
     */
    private final Date created;

    /**
     * Date when action was last updated
     */
    private final Date updated;

    /**
     * Accept and execute an implementation of {@link ActionVisitor} to operate on subtypes
     * @param visitor The implementation of the visitor
     * @param <T> Return type of operation result
     * @return The actual result of operation
     */
    public abstract <T> T accept(ActionVisitor<T> visitor);
}
