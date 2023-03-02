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

package io.appform.conductor.model.ticket.fields.impl;

import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.fields.Value;
import io.appform.conductor.model.ticket.fields.ValueVisitor;

import java.util.Date;

/**
 * Date field value
 */
public class DateValue extends Value<Date> {
    public DateValue(
            String fieldSchemaId,
            Date value,
            Date created,
            Date updated) {
        super(FieldType.DATE, fieldSchemaId, value, created, updated);
    }

    @Override
    public <T> T accept(ValueVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
