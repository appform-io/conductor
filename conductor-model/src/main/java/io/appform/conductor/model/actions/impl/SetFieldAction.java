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
import io.appform.conductor.model.ticket.fields.FieldValue;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Date;

/**
 * Set the fieldValue for a field in the ticket
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SetFieldAction extends Action {

    /**
     * The schema for the field that the fieldValue will be set in
     */
    String fieldSchemaId;

    /**
     * The fieldValue to be set.
     */
    FieldValue fieldValue;

    @Builder
    public SetFieldAction(
            String id,
            String name,
            String description,
            Date created,
            Date updated,
            String fieldSchemaId,
            FieldValue fieldValue) {
        super(ActionType.SET_FIELD, id, name, description, created, updated);
        this.fieldSchemaId = fieldSchemaId;
        this.fieldValue = fieldValue;
    }

    @Override
    public <T> T accept(ActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
