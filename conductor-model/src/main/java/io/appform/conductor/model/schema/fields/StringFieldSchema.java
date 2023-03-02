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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Date;

/**
 * Represents the schema for a string input field
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StringFieldSchema extends FieldSchema {

    /**
     * Maximum length of the input
     */
    int maxLength;

    /**
     * Regular expression to ensure field can take only matching patterns
     */
    String regex;

    /**
     * Default value to be filled in if field is not mandatory
     */
    String defaultValue;

    public StringFieldSchema(
            String id,
            String name,
            String displayName,
            String description,
            boolean required,
            FieldSchema parent,
            String visibilityCondition,
            String editableCondition,
            Date created,
            Date updated,
            int maxLength,
            String regex,
            String defaultValue) {
        super(FieldType.STRING,
              id,
              name,
              displayName,
              description,
              required,
              parent,
              visibilityCondition,
              editableCondition,
              created,
              updated);
        this.maxLength = maxLength;
        this.regex = regex;
        this.defaultValue = defaultValue;
    }

    @Override
    public <T> T accept(FieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
