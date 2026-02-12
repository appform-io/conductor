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

package io.appform.conductor.server.ui.views.tickets;

import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.fields.FieldValue;

/**
 *
 */
@lombok.Value
public class TicketFieldView {

    /**
     * Type of field
     */
    FieldType type;

    /**
     * Global id for the field schema
     */
    FieldSchema schema;

    /**
     * Actual field fieldValue
     */
    FieldValue fieldValue;

    /**
     * Is field editable
     */
    boolean editable;

    /**
     * Is field required
     */
    boolean required;
}
