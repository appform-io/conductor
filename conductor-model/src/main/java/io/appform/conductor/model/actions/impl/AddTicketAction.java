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

package io.appform.conductor.model.actions.impl;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.ActionVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Date;

/**
 * Add an {@link Action} to the ticket. Action must have been pre-configured in the system.
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AddTicketAction extends Action {

    /**
     * The id of the action to be added. The action will be executed by manual intervention.
     * The ticket and it's fields will be passed as parameters to the action during execution.
     */
    String actionId;

    @Builder
    public AddTicketAction(
            String id,
            String name,
            String description,
            Date created,
            Date updated,
            String actionId) {
        super(ActionType.ADD_TICKET_ACTION, id, name, description, created, updated);
        this.actionId = actionId;
    }

    @Override
    public <T> T accept(ActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
