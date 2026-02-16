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

import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.model.ticket.analytics.TicketListResponse;
import io.appform.conductor.console.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.*;

/**
 * View for tickets listing page
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketsView extends BaseLoggedInView {
    String workflowId;
    List<Workflow> workflows;
    Set<SubjectIDType> subIdTypes;
    List<Group> groups;
    TicketListResponse results;
    public TicketsView(User currentUser, String workflowId, List<Workflow> workflows,
                       List<Group> groups,
                       TicketListResponse results) {
        super("templates/tickets/tickets-list.hbs", currentUser);
        this.workflowId = workflowId;
        this.workflows = workflows;
        this.groups = groups;
        this.results = results;
        this.subIdTypes = new TreeSet<>(Comparator.comparing(SubjectIDType::getDisplayName));
        subIdTypes.addAll(EnumSet.allOf(SubjectIDType.class));
    }
}
