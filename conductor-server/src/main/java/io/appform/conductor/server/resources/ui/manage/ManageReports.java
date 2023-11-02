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

package io.appform.conductor.server.resources.ui.manage;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.reporting.ReportManager;
import io.appform.conductor.server.ui.views.reports.ReportListView;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 *
 */
@Path("/ui/manage/reports")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class ManageReports {

    private final ReportManager reportManager;

    @GET
    public Response renderTaskList(@Auth ConductorUser user) {
        return render(new ReportListView(user.getUserSession().getUser(),
                                         reportManager.listReports(),
                                         null,
                                         List.of()));
    }

    @POST
    public Response createReport(
            @FormParam("name") @NotEmpty @Length(max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("cqlQuery") @NotEmpty @Length(max = 4096) final String cqlQuery,
            @FormParam("cron") @NotEmpty @Length(max = 45) final String cron,
            @FormParam("recipients") @NotEmpty @Length(max = 2048) final String recipients) {
        return reportManager.create(ConductorServerUtils.lowerSnake(name),
                                    name,
                                    description,
                                    cqlQuery,
                                    Arrays.asList(recipients.split(",")),
                                    cron,
                                    Scope.GLOBAL)
                .map(report -> redirect("/manage/reports/" + report.getId()))
                .orElseThrow(() -> fail("Could not create report", "/manage/reports"));
    }

    @POST
    @Path("{reportId}/update")
    public Response updateReport(
            @PathParam("reportId") @NotEmpty @Length(max = 45) final String reportId,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("cqlQuery") @NotEmpty @Length(max = 4096) final String cqlQuery,
            @FormParam("cron") @NotEmpty @Length(max = 45) final String cron,
            @FormParam("recipients") @NotEmpty @Length(max = 2048) final String recipients) {
        return reportManager.update(
                        reportId,
                        description,
                        cqlQuery,
                        Arrays.asList(recipients.split(",")),
                        cron,
                        Scope.GLOBAL)
                .map(report -> redirect("/manage/reports/" + report.getId()))
                .orElseThrow(() -> fail("Could not update report " + reportId, "/manage/reports"));
    }

    @POST
    @Path("{reportId}/activate")
    public Response activateReport(
            @PathParam("reportId") @NotEmpty @Length(max = 45) final String reportId) {
        return reportManager.activate(reportId)
                .map(report -> redirect("/manage/reports/" + report.getId()))
                .orElseThrow(() -> fail("Could not update report " + reportId, "/manage/reports"));
    }

    @POST
    @Path("{reportId}/deactivate")
    public Response deactivateReport(
            @PathParam("reportId") @NotEmpty @Length(max = 45) final String reportId) {
        return reportManager.deactivate(reportId)
                .map(report -> redirect("/manage/reports/" + report.getId()))
                .orElseThrow(() -> fail("Could not update report " + reportId, "/manage/reports"));
    }

    @POST
    @Path("{reportId}/delete")
    public Response deleteReport(
            @PathParam("reportId") @NotEmpty @Length(max = 45) final String reportId) {
        if(reportManager.delete(reportId)) {
            return redirect("/manage/reports/");
        }
        throw fail("Could not delete report", "/manage/reports/" + reportId);
    }

    @GET
    @Path("{reportId}")
    public Response renderReportList(
            @Auth ConductorUser user,
            @PathParam("reportId") @NotEmpty @Length(max = 45) final String reportId) {
        return render(new ReportListView(user.getUserSession().getUser(),
                                         reportManager.listReports(),
                                         reportManager.get(reportId).orElse(null),
                                         reportManager.runsForReport(reportId, 20)));
    }
}
