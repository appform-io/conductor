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

package io.appform.conductor.model.ticket.filter;

import lombok.Getter;

/**
 * Different types of top level filter for a ticket
 */
public enum TicketFilterType {
    WORKFLOW_EQUALS("Workflow id matches"),
    CREATED_BY("Ticket created by user"),
    ASSIGNED_TO_GROUP("Ticket is currently assigned to group"),
    UNASSIGNED_TO_GROUP("Ticket is currently unassigned to any group"),
    ASSIGNED_TO_USER("Ticket is currently assigned to user"),
    UNASSIGNED_TO_USER("Ticket is currently unassigned to any user"),
    SUBJECT_EQUALS("Ticket is for given subject"),
    STATE_EQUALS("Ticket state equals"),
    STATE_IN("Ticket is in states"),
    PRIORITY_EQUALS("Ticket has given priority"),
    CREATED_TIME_WINDOW("Tickets created in this window"),
    UPDATED_TIME_WINDOW("Tickets updated in this window")
            ;

    @Getter
    private final String displayText;

    TicketFilterType(String displayText) {
        this.displayText = displayText;
    }
}
