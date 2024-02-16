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

import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.subject.*;
import io.appform.conductor.server.attributes.values.AttributeManager;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.ui.views.subjects.SubjectDetailsView;
import io.appform.conductor.server.ui.views.subjects.SubjectListView;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

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

    private final AttributeManager attributeManager;

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
                .map(subject -> render(
                        new SubjectDetailsView(user.getUserSession().getUser(),
                                               subject,
                                               ticketManager.ticketsForSubject(subjectId, null, 10)
                                                       .getResults(),
                                               attributeManager.read(AttributeScopeType.SUBJECT, subjectId))
                                      ))
                .orElseThrow(() -> fail("No such subject", SUBJECTS_LIST_PAGE));
    }

    @POST
    @Path("/create")
    public Response createSubject(
            @Auth ConductorUser user,
            @FormParam("name") @Length(max = 45) final String name,
            @FormParam("dob") final String dob,
            @FormParam("gender") final Gender gender) {
        val sId = UUID.randomUUID().toString();
        return subjectStore.saveSubject(List.of(), sId, name, htmlDateToDate(dob), gender)
                .map(s -> redirect("/subjects/" + sId + "/details"))
                .orElseThrow(() -> fail("Could not create subject", SUBJECTS_LIST_PAGE));
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
                                                         .dob(htmlDateToDate(dob))
                                                         .gender(gender)
                                                         .build())
                .map(s -> redirect("/subjects/" + subjectId + "/details"))
                .orElseThrow(() -> fail("No such subject", SUBJECTS_LIST_PAGE));
    }

    @POST
    @Path("/{subjectId}/id/add")
    public Response addSubjectId(
            @Auth ConductorUser user,
            @PathParam("subjectId") @Length(max = 45) final String subjectId,
            @FormParam("subjectIdType") @NotNull final SubjectIDType type,
            @FormParam("subIdSubType") final String subIdSubType,
            @FormParam("subIdValue") @NotNull @Length(max = 45) final String value,
            @FormParam("verificationStatus") @DefaultValue("UNVERIFIED") final SubjectIDVerificationStatus verificationStatus) {
        return subjectStore.saveIdentifier(subjectId,
                                           type,
                                           subIdSubType,
                                           value,
                                           verificationStatus)
                .map(s -> redirect("/subjects/" + subjectId + "/details"))
                .orElseThrow(() -> fail("Could not add ID", "/subjects/" + subjectId + "/details"));
    }

    @POST
    @Path("/{subjectId}/id/{subIdId}/primary")
    public Response makeSubjectIdPrimary(
            @Auth ConductorUser user,
            @PathParam("subjectId") @Length(max = 45) final String subjectId,
            @PathParam("subIdId") @Length(max = 45) final String subIdId) {
        return subjectStore.markIdentifierAsPrimary(subjectId, subIdId)
                .map(s -> redirect("/subjects/" + subjectId + "/details"))
                .orElseThrow(() -> fail("Could not add ID", "/subjects/" + subjectId + "/details"));
    }

    @POST
    @Path("/{subjectId}/id/{subIdId}/verify")
    public Response makeSubjectIdVerified(
            @Auth ConductorUser user,
            @PathParam("subjectId") @Length(max = 45) final String subjectId,
            @PathParam("subIdId") @Length(max = 45) final String subIdId) {
        return subjectStore.updateIdentifier(subjectId,
                                             subIdId,
                                             id -> id.withVerificationStatus(SubjectIDVerificationStatus.MANUALLY_VERIFIED))
                .map(s -> redirect("/subjects/" + subjectId + "/details"))
                .orElseThrow(() -> fail("Could not add ID", "/subjects/" + subjectId + "/details"));
    }

    @POST
    @Path("/{subjectId}/id/{subIdId}/delete")
    public Response deleteSubjectId(
            @Auth ConductorUser user,
            @PathParam("subjectId") @Length(max = 45) final String subjectId,
            @PathParam("subIdId") @Length(max = 45) final String subIdId) {
        if (subjectStore.deleteIdentifier(subjectId, subIdId)) {
            return redirect("/subjects/" + subjectId + "/details");
        }
        throw fail("Could not delete ID", "/subjects/" + subjectId + "/details");
    }

    @POST
    @Path("/{subjectId}/attributes")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateSubjectAttributes(
            @Auth ConductorUser user,
            @PathParam("subjectId") @Length(max = 45) final String subjectId,
            final MultivaluedMap<String, String> form) {
        val res = attributeManager.save(AttributeScopeType.SUBJECT, subjectId, form);
        val failures = res.getValidationResults()
                .values()
                .stream()
                .filter(vr -> vr.getStatus().equals(AttributeManager.AttributeValidationStatus.Status.FAILURE))
                .map(AttributeManager.AttributeValidationStatus.AttributeValidationResult::getMessage)
                .toList();
        if(failures.isEmpty()) {
            return redirect("/subjects/" + subjectId + "/details");
        }
        throw fail("Failed to validate attributes: " + failures, "/subjects/" + subjectId + "/details");
    }
}
