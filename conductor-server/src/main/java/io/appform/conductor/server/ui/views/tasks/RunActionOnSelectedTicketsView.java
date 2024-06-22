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

package io.appform.conductor.server.ui.views.tasks;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.events.analytics.ObjectReference;
import io.appform.conductor.model.tasks.Task;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RunActionOnSelectedTicketsView extends BaseLoggedInView {
    String workflowId;
    Collection<TicketState> states;
    List<Group> groups;
    List<Action> availableActions;
    Set<TicketPriority> priorities = EnumSet.allOf(TicketPriority.class);
    Task task;
    List<String> selectedStates;
    long updatedBeforeInMins;
    List<String> selectedGroups;
    List<TicketPriority> selectedPriorities;

    public RunActionOnSelectedTicketsView(
            User currentUser,
            String workflowId,
            Collection<TicketState> states,
            List<Group> groups,
            List<Action> availableActions,
            Task task,
            List<String> selectedStates,
            Long updatedBeforeInMins,
            List<String> selectedGroups,
            List<TicketPriority> selectedPriorities) {
        super("templates/tasks/run-action-on-tickets.hbs", currentUser,
              null != task
              ? new ObjectReference(ReferredObjectType.TASK, task.getId())
              : null);
        this.workflowId = workflowId;
        this.states = states;
        this.groups = groups;
        this.availableActions = availableActions;
        this.task = task;
        this.selectedStates = selectedStates;
        this.updatedBeforeInMins = updatedBeforeInMins;
        this.selectedGroups = selectedGroups;
        this.selectedPriorities = selectedPriorities;
    }
}
