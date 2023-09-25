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

package io.appform.conductor.server.resources.ui;

import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketAssignedToGroup;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketAssignedToUser;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketStateIn;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.workflow.WorkflowState;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.model.ticket.analytics.TicketGist;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.ui.views.HomeView;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.appform.conductor.server.utils.ConductorServerUtils.render;

/**
 *
 */
@Slf4j
@Path("/ui")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
@PermitAll
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class Home {

    private final TicketManager ticketManager;
    private final GroupStore groupStore;
    private final WorkflowStore workflowStore;

    @GET
    public Response home(@Auth ConductorUser user) {
        val groups = groupStore.findGroupsForUser(user.getUserSession().getUser().getSummary().getId())
                .stream()
                .map(Group::getId)
                .collect(Collectors.toUnmodifiableSet());
        val terminalStates = workflowStore.list(Set.of(WorkflowState.ACTIVE))
                .stream()
                .flatMap(workflow -> workflow.getStates()
                        .values()
                        .stream()
                        .filter(TicketState::isTerminal)
                        .map(TicketState::getId))
                .collect(Collectors.toUnmodifiableSet());
        val relevantOpenTickets = groups.isEmpty()
                                  ? List.<TicketGist>of()
                                  : ticketManager.search(List.of(new TicketAssignedToGroup(groups, false),
                                                                 new TicketStateIn(terminalStates, true),
                                                                 new TicketAssignedToUser(null, false)),
                                                         List.of(), null, 10).getResults();
        val myTickets = groups.isEmpty()
                        ? List.<TicketGist>of()
                        : ticketManager.search(List.of(new TicketStateIn(terminalStates, true),
                                                       new TicketAssignedToUser(user.getUserSession()
                                                                                        .getUser()
                                                                                        .getSummary()
                                                                                        .getId(), false)),
                                               List.of(), null, 10).getResults();
        return render(new HomeView(user.getUserSession().getUser(), relevantOpenTickets, myTickets));
    }
}
