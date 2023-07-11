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

package io.appform.conductor.server.schemamanagement.impl.models;

import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.workflow.Rule;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 */
@Entity
@Table(name = "boolean_field_schemas")
@Getter
@Setter
@ToString
public class StoredBooleanFieldSchema extends StoredFieldSchema {
    @Column(name = "default_value")
    private boolean defaultValue;

    public StoredBooleanFieldSchema() {
        super(FieldType.BOOLEAN);
    }

    public StoredBooleanFieldSchema(
            final String schemaId,
            final String fieldId,
            final String name,
            final String displayName,
            final String description,
            final boolean required,
            final String parent,
            final Rule visibilityCondition,
            final Rule editableCondition,
            final boolean allowMultiple,
            final boolean defaultValue) {
        super(FieldType.BOOLEAN,
              schemaId,
              fieldId,
              name,
              displayName,
              description,
              required,
              parent,
              visibilityCondition,
              editableCondition,
              allowMultiple);
        this.defaultValue = defaultValue;
    }

    @Override
    public <T> T accept(StoredFieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }


}
