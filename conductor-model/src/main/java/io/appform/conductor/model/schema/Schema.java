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

import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * Schema details for a ticket workflow
 */
@Value
public class Schema {
    /**
     * Globally unique id of the schema
     */
    String id;

    /**
     * Human-readable name of the schema
     */
    String name;

    /**
     * Human-readable description of the schema
     */
    String description;

    /**
     * Current version of the schema
     */
    long version;

    /**
     * Current state of the schema
     */
    SchemaState state;

    /**
     * Coordinates of the user that created this schema
     */
    String createdBy;

    /**
     * Coordinates of the user that updated this schema to last state
     */
    String stateChangedBy;

    /**
     * List of fields in the schema
     */
    List<FieldSchema> fields;

    /**
     * Creation date for the schema
     */
    Date created;

    /**
     * Last updated date of the schema
     */
    Date updated;
}
