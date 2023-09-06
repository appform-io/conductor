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

import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.ticketmanagement.TicketGistListResult;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.validator.constraints.Length;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static io.appform.conductor.server.resources.apis.Analytics.translateToTicketFilters;

/**
 *
 */
@Path("/tickets/v1")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Tickets {

    private final TicketManager ticketManager;

    @GET
    public ConductorApiResponse<TicketGistListResult> searchTickets(
            @Auth ConductorUser user,
            @QueryParam("workflowId") @Length(max = 45) String workflowId,
            @QueryParam("priority") final TicketPriority priority,
            @QueryParam("stateIds") @Length(max = 45) final String stateId,
            @QueryParam("subjectId") @Length(max = 45) String subjectId,
            @QueryParam("groupId") @Length(max = 45) String groupId,
            @QueryParam("createdById") @Length(max = 45) String createdById,
            @QueryParam("assignedToId") @Length(max = 45) String assignedToId,
            @QueryParam("next") @Length(max = 1024) String next,
            @QueryParam("length") @DefaultValue("100") @Min(5) @Max(200) int size) {
        val ticketFilters = translateToTicketFilters(workflowId,
                                                     priority,
                                                     stateId,
                                                     subjectId,
                                                     groupId,
                                                     createdById,
                                                     assignedToId);
        return ConductorApiResponse.success(ticketManager.search(ticketFilters, List.of(), next, size));
    }
}

