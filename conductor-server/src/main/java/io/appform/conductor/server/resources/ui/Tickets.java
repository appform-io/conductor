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

import com.google.common.base.Strings;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.model.workflow.WorkflowState;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.ticketmanagement.TicketSkeletonListResult;
import io.appform.conductor.server.ui.views.tickets.*;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 * Ticket management UI
 */
@Path("/ui/tickets")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class Tickets {
    private final WorkflowStore workflowStore;
    private final SchemaStore schemaStore;
    private final GroupStore groupStore;
    private final TicketManager ticketManager;

    @GET
    @Path("/create")
    public Response renderTicketCreateView(@Auth final ConductorUser user) {
        return render(new TicketCreateView(user.getUserSession().getUser(),
                                           workflowStore.list(Set.of(WorkflowState.ACTIVE)),
                                           TicketSkeletonListResult.EMPTY));
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createTicket(
            @Auth ConductorUser user,
            @FormParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("title") @NotEmpty @Length(max = 255) final String title,
            @FormParam("description") @DefaultValue("") @Length(max = 4096) final String description,
            @FormParam("subjectIdType") @NotNull final SubjectIDType subjectIdType,
            @FormParam("subIdSubType") @DefaultValue("") @Length(max = 255) final String subIdSubType,
            @FormParam("subIdValue") @NotEmpty @Length(max = 45) final String subIdValue) {
        return ticketManager.createTicket(
                        title,
                        description,
                        subjectIdType,
                        subIdSubType,
                        subIdValue, workflowId)
                .map(t -> redirect("/tickets/" + t.getSummary().getId() + "/details"))
                .orElseThrow(() -> fail("Could not create ticket for workflow " + workflowId,
                                        "/tickets/create"));
    }

    @GET
    @Path("/{workflowId}")
    public Response renderTicketHome(
            @Auth final ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId) {
        return render(new TicketsView(user.getUserSession().getUser(),
                                      workflowId, workflowStore.list(Set.of(WorkflowState.ACTIVE)),
                                      groupStore.list(),
                                      ticketManager.search(List.of(new TicketWorkflowEquals(workflowId)),
                                                           List.of(),
                                                           null,
                                                           1024)));
    }

    @POST
    @Path("/go")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response goToTicket(@FormParam("goToTicketId") @NotEmpty @Length(max = 45) final String ticketId) {
        return ticketManager.readTicket(ticketId)
                .map(t -> redirect("/tickets/" + ticketId + "/details"))
                .orElseThrow(() -> fail("No ticket found for ticket id: " + ticketId, "/"));
    }

    @GET
    @Path("/{ticketId}/details")
    public Response renderTicketDetails(
            @Auth final ConductorUser user,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId) {
        val ticket = ticketManager.readTicket(ticketId).orElse(null);
        if (null == ticket) {
            throw fail("No ticket found for ticket id: " + ticketId, "/");
        }
        val fieldsMap = ticket.getFields()
                .stream()
                .collect(Collectors.toMap(TicketField::getFieldSchemaId, Function.identity()));
        val wf = workflowStore.read(ticket.getSummary().getWorkflowId()).orElse(null);
        if (null == wf) {
            throw fail("No workflow found for ticket id: " + ticketId + " and workflowID: " + wf, "/");
        }
        val schema = schemaStore.get(wf.getSchemaId());
        val fieldSchemas = schema
                .map(Schema::getFields)
                .orElse(List.of());
        val ticketState = ticket.getSummary().getTicketState();
        val fields = fieldSchemas.stream()
                .filter(fs -> ticketState.getVisibleFields().contains(fs.getId()))
                .map(fs -> {
                    val existing = fieldsMap.get(fs.getId());
                    return new TicketFieldView(fs.getType(),
                                               fs,
                                               null != existing ? existing.getFieldValue() : null,
                                               ticketState.getEditableFields().contains(fs.getId()),
                                               ticketState.getRequiredFields().contains(fs.getId()));
                })
                .toList();
        return render(new TicketDetailsView(user.getUserSession().getUser(),
                                            ticket,
                                            wf,
                                            schema.get(),
                                            ticketState,
                                            fields,
                                            groupStore.list()));
    }

    @POST
    @Path("/{ticketId}/summary/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateTicket(
            @Auth final ConductorUser user,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId,
            @FormParam("title") @NotEmpty @Length(max = 255) final String title,
            @FormParam("description") @DefaultValue("") @Length(max = 4096) final String description,
            @FormParam("priority") @DefaultValue("MEDIUM") final TicketPriority priority) {
        return ticketManager.processFormSummaryUpdate(ticketId, title, description, priority)
                .map(t -> Response.seeOther(URI.create("/tickets/" + ticketId + "/details")).build())
                .orElse(Response.seeOther(URI.create("/")).build());
    }

    @POST
    @Path("/{ticketId}/group/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateTicketGroup(
            @Auth final ConductorUser user,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId,
            @FormParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        if (ticketManager.assignTicketToGroup(ticketId, groupId)) {
            return redirect("/tickets/" + ticketId + "/details");
        }
        throw fail("Could not assign ticket to group", "/tickets/" + ticketId + "/details");
    }

    @POST
    @Path("/{ticketId}/fields/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateTicketFields(
            @Auth final ConductorUser user,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId,
            final MultivaluedMap<String, String> form) {
        return ticketManager.processFormFieldUpdate(ticketId, form)
                .map(t -> Response.seeOther(URI.create("/tickets/" + ticketId + "/details")).build())
                .orElse(Response.seeOther(URI.create("/")).build());
    }

    @POST
    @Path("/{ticketId}/assign")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response assignTicketToMe(
            @Auth final ConductorUser user,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId) {
        if (ticketManager.assignTicketToUser(ticketId, user.getUserSession().getUser().getSummary().getId())) {
            return redirect("/tickets/" + ticketId + "/details");
        }
        throw fail("Could not assign ticket to user", "/tickets/" + ticketId + "/details");
    }

    @POST
    @Path("/{ticketId}/assign/{userId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response assignTicketToUser(
            @Auth final ConductorUser user,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId,
            @FormParam("userId") @NotEmpty @Length(max = 45) final String userId) {
        if (ticketManager.assignTicketToUser(ticketId, userId)) {
            return redirect("/tickets/" + ticketId + "/details");
        }
        throw fail("Could not assign ticket to user", "/tickets/" + ticketId + "/details");
    }

    @POST
    @Path("/{ticketId}/unassign")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response unassignTicketFromMe(
            @Auth final ConductorUser user,
            @HeaderParam(com.google.common.net.HttpHeaders.REFERER) final URI referrer,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId) {
        val referrerPath = null == referrer
                           ? null
                           : referrer.getPath();
        val redirectPath = referrerPath == null
                ? "/tickets/" + ticketId + "/details"
                : referrerPath.replaceAll("/apis/ui", "");
        if (ticketManager.unassignTicketFromEveryone(ticketId)) {
            return redirect(redirectPath);
        }
        throw fail("Could not unassign ticket from user", redirectPath);
    }

    @POST
    @Path("/{ticketId}/unassign/{userId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response unassignTicketToUser(
            @Auth final ConductorUser user,
            @PathParam("ticketId") @NotEmpty @Length(max = 45) final String ticketId,
            @FormParam("userId") @NotEmpty @Length(max = 45) final String userId) {
        if (ticketManager.unassignTicketFromUser(ticketId, userId)) {
            return redirect("/tickets/" + ticketId + "/details");
        }
        throw fail("Could not unassign ticket from user", "/tickets/" + ticketId + "/details");
    }

    @GET
    public Response listTickets(@Auth ConductorUser user) {
        return render(TicketSearchView.builder()
                              .currentUser(user.getUserSession().getUser())
                              .workflows(workflowStore.list(Set.of(WorkflowState.ACTIVE)))
                              .states(List.of())
                              .groups(groupStore.list())
                              .build());
    }

    @GET
    @Path("/list")
    public Response searchTickets(
            @Auth ConductorUser user,
            @QueryParam("workflowId") @Length(max = 45) String workflowId,
            @QueryParam("priority") final TicketPriority priority,
            @QueryParam("stateIds") @Length(max = 45) final String stateId,
            @QueryParam("subjectId") @Length(max = 45) String subjectId,
            @QueryParam("groupId") @Length(max = 45) String groupId,
            @QueryParam("createdById") @Length(max = 45) String createdById,
            @QueryParam("assignedToId") @Length(max = 45) String assignedToId,
            @QueryParam("next") @Length(max = 1024) String next,
            @QueryParam("size") @DefaultValue("100") @Min(10) @Max(200) int size) {
        val ticketFilters = new ArrayList<TicketFilter>();
        if (!Strings.isNullOrEmpty(workflowId)) {
            ticketFilters.add(new TicketWorkflowEquals(workflowId));
        }
        if (null != priority) {
            ticketFilters.add(new TicketPriorityEquals(priority));
        }
        if (!Strings.isNullOrEmpty(stateId)) {
            ticketFilters.add(new TicketStateEquals(stateId));
        }
        if (!Strings.isNullOrEmpty(subjectId)) {
            ticketFilters.add(new TicketSubjectEquals(subjectId));
        }
        if (!Strings.isNullOrEmpty(groupId)) {
            ticketFilters.add(new TicketAssignedToGroup(Set.of(groupId)));
        }
        if (!Strings.isNullOrEmpty(createdById)) {
            ticketFilters.add(new TicketCreatedBy(createdById));
        }
        if (!Strings.isNullOrEmpty(assignedToId)) {
            ticketFilters.add(new TicketAssignedToUser(assignedToId));
        }
        val results = ticketManager.search(ticketFilters, List.of(), next, size);
        return render(TicketSearchView.builder()
                              .currentUser(user.getUserSession().getUser())
                              .workflows(workflowStore.list(Set.of(WorkflowState.ACTIVE)))
                              .states(Strings.isNullOrEmpty(workflowId) ? List.of() : workflowStore.read(workflowId)
                                      .map(workflow -> workflow.getStates().values())
                                      .map(List::copyOf)
                                      .orElse(List.of()))
                              .groups(groupStore.list())
                              .workflowId(workflowId)
                              .stateId(stateId)
                              .priority(priority)
                              .subjectId(subjectId)
                              .groupId(groupId)
                              .createdById(createdById)
                              .assignedToId(assignedToId)
                              .results(results)
                              .build());
    }

}
