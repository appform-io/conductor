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
import io.appform.conductor.model.actions.ActionScope;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.ActionVisitor;
import io.appform.conductor.model.workflow.Template;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Date;

/**
 * Add a {@link io.appform.conductor.model.ticket.comments.Comment} to the ticket.
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AddCommentAction extends Action {

    /**
     * A {@link io.appform.conductor.model.workflow.Template} to generate comment text from ticket summary and fields.
     * This template will be evaluated to generate the {@link io.appform.conductor.model.ticket.comments.Comment} content.
     */
    Template contentTemplate;

    @Builder
    public AddCommentAction(
            String id,
            String name,
            ActionScope scope,
            String description,
            Date created,
            Date updated,
            Template contentTemplate) {
        super(ActionType.ADD_COMMENT, id, name, description, scope, created, updated);
        this.contentTemplate = contentTemplate;
    }

    @Override
    public <T> T accept(ActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
