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

package io.appform.conductor.server.resources.apis;

import com.google.common.base.Strings;
import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.TicketGroupResponse;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.validator.constraints.Length;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Path("/analytics/v1")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Analytics {

    private final TicketManager ticketManager;

    @GET
    @Path("/group")
    public ConductorApiResponse<TicketGroupResponse> groupCount(
            @Auth ConductorUser user,
            @QueryParam("workflowId") @Length(max = 45) String workflowId,
            @QueryParam("priority") final TicketPriority priority,
            @QueryParam("stateIds") @Length(max = 45) final String stateId,
            @QueryParam("subjectId") @Length(max = 45) String subjectId,
            @QueryParam("groupId") @Length(max = 45) String groupId,
            @QueryParam("createdById") @Length(max = 45) String createdById,
            @QueryParam("assignedToId") @Length(max = 45) String assignedToId,
            @QueryParam("field") @NotEmpty String ticketPropertyName) {
        val ticketFilters = translateToTicketFilters(workflowId,
                                                     priority,
                                                     stateId,
                                                     subjectId,
                                                     groupId,
                                                     createdById,
                                                     assignedToId);
        return ConductorApiResponse.success(ticketManager.groupCount(ticketFilters, List.of(), ticketPropertyName));
    }

    public static List<TicketFilter> translateToTicketFilters(
            String workflowId,
            TicketPriority priority,
            String stateId,
            String subjectId,
            String groupId,
            String createdById,
            String assignedToId) {
        val ticketFilters = new ArrayList<TicketFilter>();
        if (!Strings.isNullOrEmpty(workflowId)) {
            ticketFilters.add(new TicketWorkflowEquals(workflowId));
        }
        if (null != priority) {
            ticketFilters.add(new TicketPriorityIn(Set.of(priority), false));
        }
        if (!Strings.isNullOrEmpty(stateId)) {
            ticketFilters.add(new TicketStateIn(Set.of(stateId), false));
        }
        if (!Strings.isNullOrEmpty(subjectId)) {
            ticketFilters.add(new TicketSubjectEquals(subjectId));
        }
        if (!Strings.isNullOrEmpty(groupId)) {
            ticketFilters.add(new TicketAssignedToGroup(Set.of(groupId), false));
        }
        if (!Strings.isNullOrEmpty(createdById)) {
            ticketFilters.add(new TicketCreatedBy(createdById));
        }
        if (!Strings.isNullOrEmpty(assignedToId)) {
            ticketFilters.add(new TicketAssignedToUser(assignedToId, false));
        }
        return ticketFilters;
    }
}

