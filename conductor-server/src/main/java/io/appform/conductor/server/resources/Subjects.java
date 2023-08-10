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

package io.appform.conductor.server.resources;

import io.appform.conductor.model.subject.Gender;
import io.appform.conductor.model.subject.SubjectID;
import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.subject.SubjectSummary;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.ui.views.subjects.SubjectDetailsView;
import io.appform.conductor.server.ui.views.subjects.SubjectListView;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 *
 */
@Path("/ui/subjects")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class Subjects {
    private static final String SUBJECTS_LIST_PAGE = "/subjects/search";

    private final SubjectStore subjectStore;

    private final TicketManager ticketManager;

    @GET
    @Path("/search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renderSubjectSearchPage(@Auth ConductorUser user) {
        return render(new SubjectListView(user.getUserSession().getUser(), List.of()));
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response searchForSubject(
            @Auth ConductorUser user,
            @FormParam("subjectIdType") @NotNull final SubjectIDType type,
            @FormParam("subIdSubType") @Length(max = 45) final String subIdSubType,
            @FormParam("subIdValue") @Length(max = 45) final String subIdValue) {
        val res = subjectStore.lookupSummaryById(SubjectID.builder()
                                                         .type(type)
                                                         .subType(subIdSubType)
                                                         .value(subIdValue)
                                                         .build());
        if (res.isEmpty()) {
            throw fail("No subjects found for c=given criterion", SUBJECTS_LIST_PAGE);
        }
        return render(new SubjectListView(user.getUserSession().getUser(), res));
    }

    @POST
    @Path("/go")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response goToSubject(
            @Auth ConductorUser user,
            @FormParam("subjectId") @Length(max = 45) final String subjectId) {
        return redirect("/subjects/" + subjectId + "/details");
    }

    @GET
    @Path("/{subjectId}/details")
    public Response response(
            @Auth ConductorUser user,
            @PathParam("subjectId") @Length(max = 45) final String subjectId) {
        return subjectStore.getSubject(subjectId)
                .map(subject -> render(new SubjectDetailsView(user.getUserSession().getUser(),
                                                        subject,
                                                        ticketManager.ticketsForSubject(subjectId))))
                .orElseThrow(() -> fail("No such subject", SUBJECTS_LIST_PAGE));
    }

    @POST
    @Path("/{subjectId}/summary/update")
    public Response updateSubjectSummary(
            @Auth ConductorUser user,
            @PathParam("subjectId") @Length(max = 45) final String subjectId,
            @FormParam("name") @Length(max = 45) final String name,
            @FormParam("dob") final String dob,
            @FormParam("gender") final Gender gender) {
        return subjectStore.updateSubjectSummary(subjectId,
                                                 s -> SubjectSummary.builder()
                                                         .name(name)
                                                         .dob(ConductorServerUtils.htmlDateToDate(dob))
                                                         .gender(gender)
                                                         .build())
                .map(s -> redirect("/subjects/" + subjectId + "/details"))
                .orElseThrow(() -> fail("No such subject", SUBJECTS_LIST_PAGE));
    }
}
