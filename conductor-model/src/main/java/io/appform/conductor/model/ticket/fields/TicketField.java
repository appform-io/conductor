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

package io.appform.conductor.model.ticket.fields;

import io.appform.conductor.model.schema.FieldType;

import java.util.Date;

/**
 *
 */
@lombok.Value
public class TicketField {

    /**
     * Type of field
     */
    FieldType type;

    /**
     * Global id for the field schema
     */
    String fieldSchemaId;

    /**
     * Actual field value
     */
    Value value;

    /**
     * Creation date of the value
     */
    Date created;

    /**
     * Date when the value was last updated
     */
    Date updated;

}
