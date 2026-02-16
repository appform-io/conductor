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

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.actions.impl.AddTicketAction;
import io.appform.conductor.model.actions.impl.ChangePriorityAction;
import io.appform.conductor.model.actions.impl.RouteToGroupAction;
import io.appform.conductor.model.actions.impl.WebhookAction;
import io.appform.conductor.model.actions.impl.SetFieldAction;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.core.actionmanagement.ActionStore;
import io.appform.conductor.core.auth.ConductorUser;
import io.appform.conductor.core.schemamanagement.impl.SchemaStore;
import io.appform.conductor.console.ui.views.actions.ActionListView;
import io.appform.conductor.console.ui.views.actions.fragments.AddTicketActionFragment;
import io.appform.conductor.console.ui.views.actions.fragments.ChangePriorityActionFragment;
import io.appform.conductor.console.ui.views.actions.fragments.RouteToGroupActionFragment;
import io.appform.conductor.console.ui.views.actions.fragments.WebHookActionFragment;
import io.appform.conductor.console.ui.views.actions.fragments.SetFieldActionFragment;
import io.appform.conductor.core.interfaces.GroupStore;
import io.appform.conductor.core.utils.ConductorServerUtils;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.core.utils.Pair;
import io.appform.conductor.core.workflowmanagement.WorkflowStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.ManualErrorHandling;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static io.appform.conductor.core.utils.ConductorServerUtils.*;

/**
 *
 */
@Slf4j
@Path("/ui/actions")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
@PermitAll
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@ManualErrorHandling
public class Actions {
    private final ActionStore actionStore;
    private final WorkflowStore workflowStore;
    private final SchemaStore schemaStore;
    private final GroupStore groupStore;

    @GET
    public Response renderGlobalActions(@Auth final ConductorUser user) {
        return renderListPage(user, Scope.ScopeType.GLOBAL, Scope.GLOBAL_STATE_REF_ID);
    }

    @GET
    @Path("/{scopeType}/{referenceId}")
    public Response renderActionsList(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId) {
        return renderListPage(user, scopeType, referenceId);
    }

    @POST
    @Path("/{scopeType}/{referenceId}/{actionId}/delete")
    public Response deleteAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId) {
        if (actionStore.delete(actionId)) {
            return redirect(actionList(scopeType, referenceId));
        }
        throw fail("Could not delete action", actionList(scopeType, referenceId));
    }

    @POST
    @Path("/{scopeType}/{referenceId}/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renderFragment(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("type") @NotNull final ActionType type) {
        val scope = Scope.create(scopeType, referenceId);
        val fragment = switch (type) {
            case WEBHOOK -> new WebHookActionFragment(scope, null);
            case ROUTE_TO_GROUP -> new RouteToGroupActionFragment(groupStore.list(), scope, null);
            case ADD_COMMENT -> null;
            case ADD_TICKET_ACTION -> new AddTicketActionFragment(actionStore.listActionsForScopes(List.of(Scope.GLOBAL, scope)),
                                                                  scope,
                                                                  null);
            case CHANGE_PRIORITY -> new ChangePriorityActionFragment(scope, null);
            case SET_FIELD -> new SetFieldActionFragment(scope, null);
        };
        return render(fragment);
    }

    @GET
    @Path("/{scopeType}/{referenceId}/{actionId}")
    public Response renderFragment(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId) {
        val action = actionStore.read(actionId).orElse(null);
        if (null == action) {
            return Response.noContent().build();
        }
        val scope = Scope.create(scopeType, referenceId);
        val view = switch (action.getType()) {
            case WEBHOOK -> new WebHookActionFragment(scope, action);
            case ROUTE_TO_GROUP -> new RouteToGroupActionFragment(groupStore.list(), scope, action);
            case ADD_COMMENT -> null;
            case ADD_TICKET_ACTION -> new AddTicketActionFragment(actionStore.listActionsForScopes(List.of(Scope.GLOBAL, scope)),
                                                                  scope,
                                                                  action);
            case CHANGE_PRIORITY -> new ChangePriorityActionFragment(scope, action);
            case SET_FIELD -> new SetFieldActionFragment(scope, action);
        };
        return render(view);
    }

    @POST
    @Path("{scopeType}/{referenceId}/ROUTE_TO_GROUP/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createRouteToGroupAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        val scope = Scope.create(scopeType, referenceId);
        return actionStore.save(new RouteToGroupAction(ConductorServerUtils.generateActionId(),
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
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        val scope = Scope.create(scopeType, referenceId);
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
    @Path("{scopeType}/{referenceId}/ADD_TICKET_ACTION/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createAddTicketAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("ticketActionId") @NotEmpty @Length(max = 45) final String ticketActionId) {
        val scope = Scope.create(scopeType, referenceId);
        return actionStore.save(new AddTicketAction(ConductorServerUtils.generateActionId(),
                                                    name,
                                                    description,
                                                    scope,
                                                    null,
                                                    null,
                                                    ticketActionId))
                .map(a -> redirect(actionList(scopeType, referenceId)))
                .orElseThrow(() -> fail("Could not create action", actionList(scopeType, referenceId)));
    }

    @POST
    @Path("{scopeType}/{referenceId}/{actionId}/ADD_TICKET_ACTION/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateAddTicketAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("ticketActionId") @NotEmpty @Length(max = 45) final String ticketActionId) {
        val scope = Scope.create(scopeType, referenceId);
        if (actionStore.update(actionId,
                               action ->
                                       new AddTicketAction(action.getId(),
                                                              name,
                                                              description,
                                                              scope,
                                                              action.getCreated(),
                                                              null,
                                                              actionId))) {
            return redirect(actionList(scopeType, referenceId));
        }
        throw fail("Could not create action", actionList(scopeType, referenceId));
    }

    @POST
    @Path("{scopeType}/{referenceId}/CHANGE_PRIORITY/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createChangePriorityAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("priority") @NotNull TicketPriority priority) {
        val scope = Scope.create(scopeType, referenceId);
        return actionStore.save(new ChangePriorityAction(ConductorServerUtils.generateActionId(),
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
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("priority") @NotNull TicketPriority priority) {
        val scope = Scope.create(scopeType, referenceId);
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

    @POST
    @Path("{scopeType}/{referenceId}/WEBHOOK/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createWebhookAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("callType") @NotNull WebhookAction.CallType callType,
            @FormParam("urlTemplate") @NotEmpty @Length(max = 1024) String urlTemplate,
            @FormParam("mimeType") WebhookAction.MimeType mimeType,
            @FormParam("payloadTemplate") @Length(max = 4096) String payloadTemplate,
            @FormParam("headerTemplates") @Length(max = 4096) String headerTemplates,
            @FormParam("successCodes") @Length(max = 128) String successCodes) {
        val scope = Scope.create(scopeType, referenceId);
        return actionStore.save(new WebhookAction(ConductorServerUtils.generateActionId(),
                                                  name,
                                                  description,
                                                  scope,
                                                  null,
                                                  null,
                                                  handlebarsTemplate(urlTemplate),
                                                  callType,
                                                  handlebarsTemplate(payloadTemplate),
                                                  mimeType,
                                                  parseHeaders(headerTemplates),
                                                  parseSuccessCodes(successCodes),
                                                  WebhookAction.CallMode.ASYNC,
                                                  3000,
                                                  WebhookAction.RetryStrategy.FIXED_INTERVAL,
                                                  3))
                .map(a -> redirect(actionList(scopeType, referenceId)))
                .orElseThrow(() -> fail("Could not create action", actionList(scopeType, referenceId)));
    }

    @POST
    @Path("{scopeType}/{referenceId}/{actionId}/WEBHOOK/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateWebhookAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("callType") @NotNull WebhookAction.CallType callType,
            @FormParam("urlTemplate") @NotEmpty @Length(max = 1024) String urlTemplate,
            @FormParam("mimeType") WebhookAction.MimeType mimeType,
            @FormParam("payloadTemplate") @Length(max = 4096) String payloadTemplate,
            @FormParam("headerTemplates") @Length(max = 4096) String headerTemplates,
            @FormParam("successCodes") @Length(max = 128) String successCodes) {
        val scope = Scope.create(scopeType, referenceId);
        if (actionStore.update(actionId,
                               action -> new WebhookAction(action.getId(),
                                                           name,
                                                           description,
                                                           scope,
                                                           action.getCreated(),
                                                           null,
                                                           handlebarsTemplate(urlTemplate),
                                                           callType,
                                                           handlebarsTemplate(payloadTemplate),
                                                           mimeType,
                                                           parseHeaders(headerTemplates),
                                                           parseSuccessCodes(successCodes),
                                                           WebhookAction.CallMode.ASYNC,
                                                           3000,
                                                           WebhookAction.RetryStrategy.FIXED_INTERVAL,
                                                           3))) {
            return redirect(actionList(scopeType, referenceId));
        }
        throw fail("Could not create action", actionList(scopeType, referenceId));
    }


    @POST
    @Path("{scopeType}/{referenceId}/SET_FIELD/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createSetFieldAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("fieldSchemaName") @NotNull String fieldSchemaName,
            @FormParam("fieldValueTemplate") @NotEmpty @Length(max = 1024) String fieldValueTemplate) {
        val scope = Scope.create(scopeType, referenceId);
        return actionStore.save(new SetFieldAction(ConductorServerUtils.generateActionId(),
                        name,
                        description,
                        scope,
                        null,
                        null,
                        fieldSchemaName,
                        handlebarsTemplate(fieldValueTemplate)))
                .map(a -> redirect(actionList(scopeType, referenceId)))
                .orElseThrow(() -> fail("Could not create action", actionList(scopeType, referenceId)));
    }

    @POST
    @Path("{scopeType}/{referenceId}/{actionId}/SET_FIELD/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateSetFieldAction(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final Scope.ScopeType scopeType,
            @PathParam("referenceId") @Length(max = 45) final String referenceId,
            @PathParam("actionId") @Length(max = 45) final String actionId,
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("fieldSchemaName") @NotNull String fieldSchemaName,
            @FormParam("fieldValueTemplate") @NotEmpty @Length(max = 1024) String fieldValueTemplate) {
        val scope = Scope.create(scopeType, referenceId);
        if (actionStore.update(actionId,
                action -> new SetFieldAction(action.getId(),
                        name,
                        description,
                        scope,
                        action.getCreated(),
                        null,
                        fieldSchemaName,
                        handlebarsTemplate(fieldValueTemplate)))) {
            return redirect(actionList(scopeType, referenceId));
        }
        throw fail("Could not create action", actionList(scopeType, referenceId));
    }

    private Response renderListPage(ConductorUser user, Scope.ScopeType scopeType, String referenceId) {
        val scope = Scope.create(scopeType, referenceId);
        return render(new ActionListView(user.getUserSession().getUser(), actionStore.listActionsForScopes(List.of(scope)), scope));
    }

    private static String actionList(Scope.ScopeType scopeType, String referenceId) {
        return String.format("/actions/%s/%s", scopeType, referenceId);
    }

    private static io.appform.conductor.model.workflow.Template handlebarsTemplate(String urlTemplate) {
        return new io.appform.conductor.model.workflow.Template(io.appform.conductor.model.workflow.Template.Type.HANDLEBARS,
                                                                urlTemplate);
    }

    private static Set<Integer> parseSuccessCodes(String successCodes) {
        return Arrays.stream(successCodes.split(","))
                .map(String::trim)
                .filter(text -> text.matches("\\d{3}"))
                .map(Integer::parseInt)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, io.appform.conductor.model.workflow.Template> parseHeaders(String headerTemplates) {
        return Arrays.stream(headerTemplates.trim().split("\\r?\\n"))
                .map(headerStr -> {
                    val parts = headerStr.split(":");
                    if (parts.length < 2) {
                        return null;
                    }
                    return new Pair<>(parts[0].trim(), handlebarsTemplate(parts[1].trim())
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

}
