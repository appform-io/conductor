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

package io.appform.conductor.model.schema.fields;

import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldSchemaVisitor;
import io.appform.conductor.model.schema.FieldType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;

/**
 * Represents the schema for numeric input field
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NumberFieldSchema extends FieldSchema {

    /**
     * Max bound (inclusive) for the input
     */
    private double max;

    /**
     * Min bound (inclusive) for the input
     */
    private double min;

    /**
     * Default value to be filled in if field is not mandatory
     */
    private double defaultValue;

    public NumberFieldSchema(
            String id,
            String name,
            String displayName,
            String description,
            boolean required,
            String parent,
            String visibilityCondition,
            String editableCondition,
            boolean allowMultiple,
            Date created,
            Date updated,
            double max,
            double min,
            double defaultValue) {
        super(FieldType.NUMBER,
              id,
              name,
              displayName,
              description,
              required,
              parent,
              visibilityCondition,
              editableCondition,
              allowMultiple,
              created,
              updated);
        this.max = max;
        this.min = min;
        this.defaultValue = defaultValue;
    }

    @Override
    public <T> T accept(FieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
