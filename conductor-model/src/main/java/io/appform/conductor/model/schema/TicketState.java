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
 * A representation for a ticket state
 */
@Value
public class TicketState {

    /**
     * Global ID for the state
     */
    String id;

    /**
     * Human-readable name
     */
    String displayName;

    /**
     * Human-readable description for the state
     */
    String description;

    /**
     * If this is a terminal state
     */
    boolean terminal;

    /**
     * Actions allowed in a state
     */
    List<String> allowedActions;

    /**
     * Field ids for fields that are allowed in that state
     */
    List<String> editableFields;

    /**
     * Fields ids for fields visible in a state
     */
    List<String> visibleFields;

    /**
     * Creation date of the state
     */
    Date created;

    /**
     * Last update date of the state
     */
    Date updated;
}
