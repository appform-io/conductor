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

package io.appform.conductor.model.ticket;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.ticket.fields.TicketField;

import java.util.List;

/**
 * Detailed ticket information
 */
@lombok.Value
public class TicketDetails {

    /**
     * Summary for the ticket
     */
    TicketSummary summary;

    /**
     * List of fields for this ticket.
     * Each field is governed by the corresponding {@link io.appform.conductor.model.schema.FieldSchema}.
     */
    List<TicketField> fields;

    /**
     * Ticket specific manual actions. For example a API call to be triggered manually for outbound calling.
     */
    List<Action> ticketAction;
}
