/*
 * Copyright (c) 2023 Santanu Sinha
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

package io.appform.conductor.core.schemamanagement.impl.models;

import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.workflow.Rule;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 *
 */
@Entity
@Getter
@Setter
@ToString
@DiscriminatorValue(value = FieldType.NUMBER_TEXT)
public class StoredNumberFieldSchema extends StoredFieldSchema {
    @Column(name = "max_value")
    private double max;

    @Column(name = "min_value")
    private double min;

    @Column(name = "default_number")
    private double defaultNumber;

    public StoredNumberFieldSchema() {
        super(FieldType.NUMBER);
    }

    public StoredNumberFieldSchema(
            final String schemaId,
            final String fieldId,
            final String name,
            final String displayName,
            final String description,
            final String parent,
            final Rule visibilityCondition,
            final Rule editableCondition,
            final boolean allowMultiple,
            final double max,
            final double min,
            final double defaultNumber) {
        super(FieldType.NUMBER,
              schemaId,
              fieldId,
              name,
              displayName,
              description,
              parent,
              visibilityCondition,
              editableCondition,
              allowMultiple);
        this.max = max;
        this.min = min;
        this.defaultNumber = defaultNumber;
    }

    @Override
    public <T> T accept(StoredFieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
