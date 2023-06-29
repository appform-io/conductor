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
import io.appform.conductor.model.ticket.fields.FieldValue;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 * Numeric field value
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NumberFieldValue extends FieldValue {
    Number value;
    public NumberFieldValue(Number value) {
        super(FieldType.NUMBER);
        this.value = value;
    }

    @Override
    public <T> T accept(FieldValueVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
