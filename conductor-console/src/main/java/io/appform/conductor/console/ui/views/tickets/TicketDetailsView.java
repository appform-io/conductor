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

package io.appform.conductor.console.ui.views.tickets;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.events.analytics.ObjectReference;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.core.ticketmanagement.RelatedTicketSummary;
import io.appform.conductor.console.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketDetailsView extends BaseLoggedInView {
    TicketDetails ticket;
    Workflow workflow;
    Schema schema;
    TicketState state;
    List<TicketFieldView> fields;
    List<Group> availableGroups;
    List<Action> visibleActions;
    List<RelatedTicketSummary> relatedTickets;
    Set<TicketPriority> priorities = EnumSet.allOf(TicketPriority.class);
    public TicketDetailsView(
            User currentUser,
            TicketDetails ticket,
            Workflow workflow,
            Schema schema,
            TicketState state,
            List<TicketFieldView> fields,
            List<Group> availableGroups,
            List<Action> visibleActions,
            List<RelatedTicketSummary> relatedTickets) {
        super("templates/tickets/ticket-details.hbs", currentUser,
              new ObjectReference(ReferredObjectType.TICKET, ticket.getSummary().getId()));
        this.ticket = ticket;
        this.workflow = workflow;
        this.schema = schema;
        this.state = state;
        this.fields = fields;
        this.availableGroups = availableGroups;
        this.visibleActions = visibleActions;
        this.relatedTickets = relatedTickets;
    }
}
