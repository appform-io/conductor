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

import io.appform.conductor.model.ticket.filter.ticketfilters.*;

/**
 * Needed to handle subclass specific behaviour for {@link TicketFilter}
 */
public interface TicketFilterVisitor<T> {
    T visit(TicketWorkflowEquals workflowEquals);

    T visit(TicketCreatedBy createdBy);

    T visit(TicketAssignedToGroup assignedToGroup);

    T visit(TicketUnAssignedToGroup unAssignedToGroup);

    T visit(TicketAssignedToUser assignedToUser);

    T visit(TicketUnAssignedToUser unAssignedToUser);

    T visit(TicketSubjectEquals subjectEquals);

    T visit(TicketStateIn stateIn);

    T visit(TicketPriorityIn priorityEquals);

    T visit(TicketsCreatedTimeWindow createdTimeWindow);

    T visit(TicketsUpdatedTimeWindow updatedTimeWindow);

    T visit(TicketReferenceEquals ticketReferenceEquals);

}
