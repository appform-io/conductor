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
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 *
 */
@Entity
@Getter
@Setter
@ToString
@DiscriminatorValue(value = FieldType.STRING_TEXT)
public class StoredStringFieldSchema extends StoredFieldSchema {
    @Column(name = "max_length")
    private int maxLength;

    @Column(name = "match_pattern", length = 255)
    private String matchPattern;

    @Column(name = "default_string", length = 255)
    private String defaultString;

    public StoredStringFieldSchema() {
        super(FieldType.STRING);
    }

    public StoredStringFieldSchema(
            final String schemaId,
            final String fieldId,
            final String name,
            final String displayName,
            final String description,
            final String parent,
            final Rule visibilityCondition,
            final Rule editableCondition,
            final boolean allowMultiple,
            final int maxLength,
            final String matchPattern,
            final String defaultString) {
        super(FieldType.STRING,
              schemaId,
              fieldId,
              name,
              displayName,
              description,
              parent,
              visibilityCondition,
              editableCondition,
              allowMultiple);
        this.maxLength = maxLength;
        this.matchPattern = matchPattern;
        this.defaultString = defaultString;
    }

    @Override
    public <T> T accept(StoredFieldSchemaVisitor<T> visitor) {
        return visitor.visit(this);
    }


}
