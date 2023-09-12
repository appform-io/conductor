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

package io.appform.conductor.model.schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.model.workflow.Rule;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * Definition for a field in the ticket. Will be used to render and validate inputs/outputs
 */
@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "STRING", value = StringFieldSchema.class),
        @JsonSubTypes.Type(name = "CHOICE", value = ChoiceFieldSchema.class),
        @JsonSubTypes.Type(name = "BOOLEAN", value = BooleanFieldSchema.class),
        @JsonSubTypes.Type(name = "LOCATION", value = LocationFieldSchema.class),
        @JsonSubTypes.Type(name = "DATE", value = DateFieldSchema.class),
        @JsonSubTypes.Type(name = "NUMBER", value = NumberFieldSchema.class),
})
public abstract class FieldSchema {
    /**
     * Type of field
     */
    private final FieldType type;

    /**
     * Globally unique id for the field
     */
    private final String id;

    /**
     * Normalized field name to be used for serialization formats
     */
    private final String name;

    /**
     * Field name to be used for display purposes
     */
    private String displayName;

    /**
     * Human-readable description for the field
     */
    private String description;

    /**
     * This is possible if field is child of another field (TODO)
     */
    private String parent;

    //TODO::HINT

    /**
     * Rule to configure when the field is visible
     */
    private Rule visibilityCondition;

    /**
     * Rule to configure when field is editable
     */
    private Rule editableCondition;

    /**
     * Is multiple selection allowed
     */
    private boolean allowMultiple;

    /**
     * Creation time for the schema
     */
    private final Date created;

    /**
     * Last updated timestamp
     */
    private final Date updated;


    /**
     * Accept an implementation of {@link FieldSchemaVisitor} for type specific handling like validations, rendering etc
     * @param visitor The actual implementation
     * @param <T> Return type of processing
     * @return The result of processing
     */
    public abstract <T> T accept(final FieldSchemaVisitor<T> visitor);
}
