/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.model.attributes.definition;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.attributes.AttributeType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;

import java.util.Date;

/**
 *
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AttributeDefinition {
    /**
     * Type of attribute
     */
    private final AttributeType type;

    /**
     * Globally unique id for the attribute
     */
    private final String id;

    /**
     * Normalized attribute name to be used for serialization formats
     */
    private final String name;

    /**
     * attribute name to be used for display purposes
     */
    @With
    private final String displayName;

    /**
     * Human-readable description for the attribute
     */
    @With
    private final String description;

    /**
     * Creation time for the schema
     */
    private final Date created;

    /**
     * Last updated timestamp
     */
    private final Date updated;
    
    public abstract <T> T accept(final AttributeDefinitionVisitor<T> visitor);
}
