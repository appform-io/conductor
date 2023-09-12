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
import io.appform.conductor.model.workflow.Rule;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Date;

/**
 * Represents the schema for a boolean ticket field that can only
 * tale {@link Boolean#TRUE} and {@link Boolean#FALSE} values
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BooleanFieldSchema extends FieldSchema {

    /**
     * Default value of the field if not a required field
     */
    private boolean defaultValue;

    @Jacksonized
    @Builder
    @SuppressWarnings("java:S107")
    public BooleanFieldSchema(
            String id,
            String name,
            String displayName,
            String description,
            String parent,
            Rule visibilityCondition,
            Rule editableCondition,
            boolean allowMultiple,
            Date created,
            Date updated,
            boolean defaultValue) {
        super(FieldType.BOOLEAN,
              id,
              name,
              displayName,
              description,
              parent,
              visibilityCondition,
              editableCondition,
              allowMultiple,
              created,
              updated);
        this.defaultValue = defaultValue;
    }

    @Override
    public <T> T accept(FieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
