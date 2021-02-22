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
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.schema.FieldSchemaVisitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * Represents the schema for a field with user-selectable pre-defined choices.
 * Field will be able to support single and multiple choices as configured.
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ChoiceFieldSchema extends FieldSchema {

    /**
     * List of available choices
     */
    List<String> choices;

    /**
     * Is multiple selection allowed
     */
    boolean allowMultiple;

    /**
     * Default choice if field is not mandatory
     */
    String defaultValue;

    public ChoiceFieldSchema(
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
            List<String> choices,
            boolean allowMultiple,
            String defaultValue) {
        super(FieldType.CHOICE,
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
        this.choices = choices;
        this.allowMultiple = allowMultiple;
        this.defaultValue = defaultValue;
    }

    @Override
    public <T> T accept(FieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
