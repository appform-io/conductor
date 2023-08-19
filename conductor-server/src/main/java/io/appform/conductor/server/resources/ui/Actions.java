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

import io.appform.conductor.model.actions.ActionScope;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.impl.ChangePriorityAction;
import io.appform.conductor.model.actions.impl.RouteToGroupAction;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.ui.views.actions.ActionListView;
import io.appform.conductor.server.ui.views.actions.fragments.ChangePriorityActionFragment;
import io.appform.conductor.server.ui.views.actions.fragments.RouteToGroupActionFragment;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 *
 */
@Slf4j
@Path("/ui/actions")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
@PermitAll
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Actions {
    private final ActionStore actionStore;
    private final GroupStore groupStore;

    @GET
    public Response renderGlobalActions(@Auth final ConductorUser user) {
        return renderListPage(user, ActionScope.ScopeType.GLOBAL, ActionScope.GLOBAL_STATE_REF_ID);
    }

    @GET
    @Path("/{scopeType}/{referenceId}")
    public Response renderActionsList(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId) {
        return renderListPage(user, scopeType, referenceId);
    }

    @POST
    @Path("/{scopeType}/{referenceId}/{actionId}/delete")
    public Response deleteAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId) {
        if (actionStore.delete(actionId)) {
            return redirect(actionList(scopeType, referenceId));
        }
        throw fail("Could not delete action", actionList(scopeType, referenceId));
    }

    @GET
    @Path("/fragments/{scopeType}/{referenceId}/{type}/create")
    public Response renderFragment(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("type") @NotNull final ActionType type) {
        val scope = ActionScope.build(scopeType, referenceId);
        val fragment = switch (type) {
            case WEBHOOK -> null;
            case ROUTE_TO_GROUP -> new RouteToGroupActionFragment(groupStore.list(), scope, null);
            case ADD_COMMENT -> null;
            case ADD_TICKET_ACTION -> null;
            case CHANGE_PRIORITY -> new ChangePriorityActionFragment(scope, null);
            case SET_FIELD -> null;
        };
        return render(fragment);
    }

    @GET
    @Path("/fragments/{scopeType}/{referenceId}/{actionId}")
    public Response renderFragment(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId) {
        val action = actionStore.read(actionId).orElse(null);
        if (null == action) {
            return Response.noContent().build();
        }
        val scope = ActionScope.build(scopeType, referenceId);
        val fragment = switch (action.getType()) {
            case WEBHOOK -> null;
            case ROUTE_TO_GROUP -> new RouteToGroupActionFragment(groupStore.list(), scope, action);
            case ADD_COMMENT -> null;
            case ADD_TICKET_ACTION -> null;
            case CHANGE_PRIORITY -> new ChangePriorityActionFragment(scope, action);
            case SET_FIELD -> null;
        };
        return render(fragment);
    }

    @POST
    @Path("{scopeType}/{referenceId}/ROUTE_TO_GROUP/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createRouteToGroupAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        val scope = ActionScope.build(scopeType, referenceId);
        return actionStore.save(new RouteToGroupAction(UUID.randomUUID().toString(),
                                                       name,
                                                       description,
                                                       scope,
                                                       null,
                                                       null,
                                                       groupId))
                .map(a -> redirect(actionList(scopeType, referenceId)))
                .orElseThrow(() -> fail("Could not create action", actionList(scopeType, referenceId)));
    }

    @POST
    @Path("{scopeType}/{referenceId}/{actionId}/ROUTE_TO_GROUP/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateRouteToGroupAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        val scope = ActionScope.build(scopeType, referenceId);
        if (actionStore.update(actionId,
                               action ->
                                       new RouteToGroupAction(action.getId(),
                                                              name,
                                                              description,
                                                              scope,
                                                              action.getCreated(),
                                                              null,
                                                              groupId))) {
            return redirect(actionList(scopeType, referenceId));
        }
        throw fail("Could not create action", actionList(scopeType, referenceId));
    }


    @POST
    @Path("{scopeType}/{referenceId}/CHANGE_PRIORITY/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createChangePriorityAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("priority") @NotNull TicketPriority priority) {
        val scope = ActionScope.build(scopeType, referenceId);
        return actionStore.save(new ChangePriorityAction(UUID.randomUUID().toString(),
                                                         name,
                                                         description,
                                                         scope,
                                                         null,
                                                         null,
                                                         priority))
                .map(a -> redirect(actionList(scopeType, referenceId)))
                .orElseThrow(() -> fail("Could not create action", actionList(scopeType, referenceId)));
    }

    @POST
    @Path("{scopeType}/{referenceId}/{actionId}/CHANGE_PRIORITY/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateChangePriorityAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final ActionScope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("priority") @NotNull TicketPriority priority) {
        val scope = ActionScope.build(scopeType, referenceId);
        if (actionStore.update(actionId,
                               action ->
                                       new ChangePriorityAction(action.getId(),
                                                              name,
                                                              description,
                                                              scope,
                                                              action.getCreated(),
                                                              null,
                                                              priority))) {
            return redirect(actionList(scopeType, referenceId));
        }
        throw fail("Could not create action", actionList(scopeType, referenceId));
    }

    private Response renderListPage(ConductorUser user, ActionScope.ScopeType scopeType, String referenceId) {
        val scope = ActionScope.build(scopeType, referenceId);
        return render(new ActionListView(user.getUserSession().getUser(), actionStore.list(List.of(scope)), scope));
    }

    private static String actionList(ActionScope.ScopeType scopeType, String referenceId) {
        return String.format("/actions/%s/%s", scopeType, referenceId);
    }
}
