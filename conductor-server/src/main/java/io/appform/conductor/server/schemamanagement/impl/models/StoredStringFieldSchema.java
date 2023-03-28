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
@Table(name = "string_field_schemas")
@Getter
@Setter
@ToString
public class StoredStringFieldSchema extends StoredFieldSchema {
    @Column(name = "max_length")
    private int maxLength;

    @Column(name = "match_pattern")
    private String matchPattern;

    @Column(name = "default_value")
    private String defaultValue;

    public StoredStringFieldSchema() {
        super(FieldType.STRING);
    }

    public StoredStringFieldSchema(
            String schemaId,
            String fieldId,
            String name,
            String displayName,
            String description,
            boolean required,
            String parent,
            String visibilityCondition,
            String editableCondition,
            boolean allowMultiple,
            int maxLength,
            String matchPattern,
            String defaultValue) {
        super(FieldType.STRING,
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
        this.maxLength = maxLength;
        this.matchPattern = matchPattern;
        this.defaultValue = defaultValue;
    }

    @Override
    public <T> T accept(StoredFieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }


}
