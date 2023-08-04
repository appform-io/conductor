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

import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.server.ticketmanagement.TicketSkeletonListResult;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
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
public class TicketCreateView extends BaseLoggedInView {
    List<Workflow> workflows;
    Set<SubjectIDType> subIdTypes;
    TicketSkeletonListResult results;
    public TicketCreateView(User currentUser, List<Workflow> workflows, TicketSkeletonListResult results) {
        super("templates/tickets/ticket-create.hbs", currentUser);
        this.workflows = workflows;
        this.results = results;
        this.subIdTypes = new TreeSet<>(Comparator.comparing(SubjectIDType::getDisplayName));
        subIdTypes.addAll(EnumSet.allOf(SubjectIDType.class));
    }
}
