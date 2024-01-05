/*
 * Copyright (c) 2023 santanu
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.dashboards.DashboardStore;
import io.appform.conductor.server.dashboards.model.DashboardRow;
import io.appform.conductor.server.dashboards.model.DashboardSpec;
import io.appform.conductor.server.dashboards.model.DashboardWidget;
import io.appform.conductor.server.dashboards.model.SpecVersion;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.ui.views.manage.DashboardListView;
import io.appform.conductor.server.ui.views.manage.DashboardView;
import io.appform.conductor.server.ui.views.manage.NewDashboardView;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 *
 */
@Path("/ui/dashboards")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class Dashboards {

    private final DashboardStore dashboardStore;
    private final ObjectMapper mapper;
    private final CQLEngine cqlEngine;

    @GET
    public Response renderDashboardList(@Auth ConductorUser user) {
        return render(new DashboardListView(user.getUserSession().getUser(),
                                            dashboardStore.list()));
    }

    @GET
    @Path("/new")
    @RolesAllowed(Permission.Values.MANAGE_DASHBOARD)
    public Response createDashboard(@Auth ConductorUser user) {
        return render(new NewDashboardView(user.getUserSession().getUser()));
    }

    @POST
    @Path("/create")
    @RolesAllowed(Permission.Values.MANAGE_DASHBOARD)
    public Response createDashboard(
            @FormParam("name") @NotEmpty @Length(max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description) {
        val id = lowerSnake(name);
        return dashboardStore.create(id, name, description)
                .map(dashboard -> redirect("/dashboards/" + dashboard.getId()))
                .orElseThrow(() -> fail("Could not create dashboard", "/dashboards"));
    }

    @POST
    @Path("{dashboardId}/update")
    @RolesAllowed(Permission.Values.MANAGE_DASHBOARD)
    @SneakyThrows
    public Response updateDashboard(
            @PathParam("dashboardId") @NotEmpty @Length(max = 45) final String dashboardId,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("spec") @NotEmpty @Length(max = 4096) final String specRepresentation) {
        return dashboardStore.update(
                        dashboardId,
                        description,
                        SpecVersion.V1,
                        mapper.readValue(specRepresentation, DashboardSpec.class))
                .map(dashboard -> redirect("/dashboards/" + dashboard.getId()))
                .orElseThrow(() -> fail("Could not update dashboard " + dashboardId, "/dashboards/" + dashboardId));
    }


    @POST
    @Path("{dashboardId}/delete")
    @RolesAllowed(Permission.Values.MANAGE_DASHBOARD)
    public Response deleteDashboard(
            @PathParam("dashboardId") @NotEmpty @Length(max = 45) final String dashboardId) {
        if (dashboardStore.delete(dashboardId)) {
            return redirect("/dashboards");
        }
        throw fail("Could not delete dashboard", "/dashboards");
    }

    @GET
    @Path("{dashboardId}")
    public Response renderDashboard(
            @Auth ConductorUser user,
            @PathParam("dashboardId") @NotEmpty @Length(max = 45) final String dashboardId) {
        return dashboardStore.read(dashboardId)
                .map(dashboard -> render(new DashboardView(user.getUserSession().getUser(), dashboard)))
                .orElseThrow(() -> fail("Could not find dashboard " + dashboardId, "/dashboards"));
    }

    @POST
    @Path("{dashboardId}/widget")
    @RolesAllowed(Permission.Values.MANAGE_DASHBOARD)
    public Response addWidget(
            @Auth ConductorUser user,
            @PathParam("dashboardId") @NotEmpty @Length(max = 45) final String dashboardId,
            @FormParam("widgetTitle") @Length(max = 45) final String widgetTitle,
            @FormParam("widgetCql") @Length(max = 1024) final String widgetCql) {
        val existing = dashboardStore.read(dashboardId).orElse(null);
        if(null == existing) {
            throw fail("Could not find dashboard " + dashboardId, "/dashboards");
        }
        val widget = new DashboardWidget(UUID.randomUUID().toString(),
                                         widgetTitle,
                                         DashboardWidget.QueryType.CQL,
                                         widgetCql,
                                         12,
                                         Map.of());
        val rows = Objects.requireNonNullElse(existing.getSpec().getRows(), List.<DashboardRow>of());
        val newRows = ImmutableList.<DashboardRow>builder()
                .addAll(rows)
                .add(new DashboardRow(List.of(widget)))
                .build();
        return dashboardStore.update(dashboardId,
                              existing.getDescription(),
                              existing.getSpecVersion(),
                              new DashboardSpec(newRows))
                .map(dashboard -> redirect("/dashboards/" + dashboard.getId()))
                .orElseThrow(() -> fail("Could not create dashboard", "/dashboards"));
    }

    @POST
    @Path("{dashboardId}/widget/{widgetId}/update")
    @RolesAllowed(Permission.Values.MANAGE_DASHBOARD)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateWidget(
            @Auth ConductorUser user,
            @PathParam("dashboardId") @NotEmpty @Length(max = 45) final String dashboardId,
            @PathParam("widgetId") @NotEmpty @Length(max = 45) final String widgetId,
            MultivaluedMap<String, String> form) {
        val existing = dashboardStore.read(dashboardId).orElse(null);
        if(null == existing) {
            throw fail("Could not find dashboard " + dashboardId, "/dashboards");
        }
        val widgetTitle = form.getFirst(widgetId + "-widgetTitle");
        val widgetCql = form.getFirst(widgetId + "-widgetCql");
        if(Strings.isNullOrEmpty(widgetTitle)
                || Strings.isNullOrEmpty(widgetCql)
        || widgetTitle.length() > 45
        || widgetCql.length() > 1024) {
            throw fail("Could not update widget on " + dashboardId + ". Invalid parameters", "/dashboards/" + dashboardId);
        }
        val newRows = new ArrayList<DashboardRow>();
        val rows = Objects.requireNonNullElse(existing.getSpec().getRows(), List.<DashboardRow>of());

        for (val row : rows) {
            val newWidgets = row.getWidgets()
                    .stream()
                    .map(widget -> {
                        if(!widget.getId().equals(widgetId)) {
                            return widget;
                        }
                        return new DashboardWidget(widget.getId(),
                                                   widgetTitle,
                                                   widget.getQueryType(),
                                                   widgetCql,
                                                   widget.getColWidth(),
                                                   widget.getExtraMeta());
                    })
                    .toList();
            if(!newWidgets.isEmpty()) {
                newRows.add(new DashboardRow(newWidgets));
            }
        }

        return dashboardStore.update(dashboardId,
                              existing.getDescription(),
                              existing.getSpecVersion(),
                              new DashboardSpec(newRows))
                .map(dashboard -> redirect("/dashboards/" + dashboard.getId()))
                .orElseThrow(() -> fail("Could not update dashboard", "/dashboards/" + dashboardId));
    }

    @POST
    @Path("{dashboardId}/widget/{widgetId}/delete")
    @RolesAllowed(Permission.Values.MANAGE_DASHBOARD)
    public Response deleteWidget(
            @Auth ConductorUser user,
            @PathParam("dashboardId") @NotEmpty @Length(max = 45) final String dashboardId,
            @PathParam("widgetId") @NotEmpty @Length(max = 45) final String widgetId) {
        val existing = dashboardStore.read(dashboardId).orElse(null);
        if(null == existing) {
            throw fail("Could not find dashboard " + dashboardId, "/dashboards");
        }
        val rows = Objects.requireNonNullElse(existing.getSpec().getRows(), List.<DashboardRow>of());
        val newRows = new ArrayList<DashboardRow>();
        for (val row : rows) {
            val newWidgets = row.getWidgets()
                    .stream()
                    .filter(widget -> !widget.getId().equals(widgetId))
                    .toList();
            if(!newWidgets.isEmpty()) {
                newRows.add(new DashboardRow(newWidgets));
            }
        }
        return dashboardStore.update(dashboardId,
                              existing.getDescription(),
                              existing.getSpecVersion(),
                              new DashboardSpec(newRows))
                .map(dashboard -> redirect("/dashboards/" + dashboard.getId()))
                .orElseThrow(() -> fail("Could not update dashboard", "/dashboards/" + dashboardId));
    }
}
