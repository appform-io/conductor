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

import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.model.workflow.WorkflowState;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.ui.views.manage.*;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.workflowmanagement.WorkflowManager;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.text.CaseUtils;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 * Administration ui
 */
@Path("/ui/manage")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class Manage {
    private final SchemaStore schemaStore;
    private final WorkflowStore workflowStore;
    private final WorkflowManager workflowManager;

    @GET
    @Path("/schema")
    public Response renderSchemaList(@Auth ConductorUser user) {
        return render(new SchemaListView(user.getUserSession().getUser(), schemaStore.list()));
    }

    @GET
    @Path("/schema/create")
    public Response renderSchemaCreate(@Auth ConductorUser user) {
        return render(new SchemaView(user.getUserSession().getUser(), null));
    }

    @POST
    @Path("/schema/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createSchema(
            @Auth ConductorUser user,
            @FormParam("name") @NotEmpty @Length(max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description) {
        return schemaStore.create(name, description)
                .flatMap(schemaSummary -> schemaStore.updateState(schemaSummary.getId(), SchemaState.ACTIVE))
                .map(schemaSummary -> redirect("/manage/schema/" + schemaSummary.getId()))
                .orElseThrow(() -> fail("Could not create state", "/manage/schema"));
    }


    @POST
    @Path("/schema/update/{schemaId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateSchemaDescription(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @FormParam("description") @Length(max = 255) final String description) {
        return schemaStore.updateDescription(schemaId, description)
                .map(schemaSummary -> redirect("/manage/schema/" + schemaSummary.getId()))
                .orElseThrow(() -> fail("Failed to update schema " + schemaId, "/manage/schema"));
    }

    @GET
    @Path("/schema/{schemaId}")
    public Response renderSchemaDetails(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId) {
        return schemaStore.get(schemaId)
                .map(schema -> render(new SchemaView(user.getUserSession().getUser(), schema)))
                .orElseThrow(() -> fail("Failed to find schema " + schemaId, "/manage/schema"));
    }

    @POST
    @Path("/schema/{schemaId}/fields/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addField(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @FormParam("fieldName") @NotEmpty @Length(max = 45) final String fieldName,
            @FormParam("fieldDescription") @Length(max = 255) final String fieldDescription,
            @FormParam("fieldType") @NotNull final FieldType fieldType,
            @FormParam("fieldRequired") @DefaultValue("false") final boolean fieldRequired,
            @FormParam("fieldStringMaxLength") @Min(1) @Max(255) final int fieldStringMaxLength,
            @FormParam("fieldStringRegex") final String fieldStringRegex,
            @FormParam("fieldChoiceChoices") final String fieldChoiceChoices,
            @FormParam("fieldChoiceMulti") @DefaultValue("false") final boolean fieldChoiceMullti,
            @FormParam("fieldNumberMin") final double fieldNumberMin,
            @FormParam("fieldNumberMax") final double fieldNumberMax) {
        val name = CaseUtils.toCamelCase(fieldName.trim(), false, ' ');
        //TODO::DEFAULT VALUE TO BE PREFILLED
        val fs = switch (fieldType) {
            case STRING -> StringFieldSchema.builder()
                    .name(name)
                    .displayName(fieldName)
                    .description(fieldDescription)
                    .required(fieldRequired)
                    .maxLength(fieldStringMaxLength)
                    .matchPattern(fieldStringRegex)
                    .build();
            case CHOICE -> ChoiceFieldSchema.builder()
                    .name(name)
                    .displayName(fieldName)
                    .description(fieldDescription)
                    .allowMultiple(fieldChoiceMullti)
                    .choices(Arrays.stream(fieldChoiceChoices.split(","))
                                     .map(choice -> new ChoiceFieldSchema.Option(upperSnake(choice), choice))
                                     .toList())
                    .build();
            case BOOLEAN -> BooleanFieldSchema.builder()
                    .name(name)
                    .displayName(fieldName)
                    .description(fieldDescription)
                    .build();
            case NUMBER -> NumberFieldSchema.builder()
                    .name(name)
                    .displayName(fieldName)
                    .description(fieldDescription)
                    .min(fieldNumberMin)
                    .max(fieldNumberMax)
                    .build();
            case LOCATION -> LocationFieldSchema.builder()
                    .name(name)
                    .displayName(fieldName)
                    .description(fieldDescription)
                    .build();
            case DATE -> DateFieldSchema.builder()
                    .name(name)
                    .displayName(fieldName)
                    .description(fieldDescription)
                    .build();
        };
        return schemaStore.addField(schemaId, schemaId + "-" + name, fs)
                .map(f -> redirect("/manage/schema/" + schemaId))
                .orElseThrow(() -> fail("Failed to add field to schema " + schemaId, "/manage/schema"));
    }

    @POST
    @Path("/schema/{schemaId}/fields/{fieldId}/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteField(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @PathParam("fieldId") @NotEmpty @Length(max = 45) final String fieldId) {
        if(schemaStore.deleteField(schemaId, fieldId)) {
            return redirect("/manage/schema/" + schemaId);
        }
        throw fail("Failed to remove field from schema " + schemaId, "/manage/schema");
    }

    @GET
    @Path("/workflow")
    public Response renderWorkflowList(@Auth ConductorUser user) {
        return render(new WorkflowListView(user.getUserSession().getUser(),
                                           workflowStore.list(EnumSet.allOf(WorkflowState.class))));
    }

    @GET
    @Path("/workflow/create")
    public Response renderWorkflowCreateScreen(@Auth ConductorUser user) {
        return render(new WorkflowCreateView(user.getUserSession().getUser(),
                                             schemaStore.list()
                                                     .stream()
                                                     .filter(schemaSummary -> schemaSummary.getState()
                                                             .equals(SchemaState.ACTIVE))
                                                     .toList()));
    }

    @POST
    @Path("/workflow/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createWorkflow(
            @Auth ConductorUser user,
            @FormParam("name") @NotEmpty @Length(max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("schemaId") @NotEmpty @Length(max = 255) final String schemaId) {
        return workflowStore.create(lowerSnake(name),
                                    name,
                                    description,
                                    schemaId,
                                    null,
                                    null,
                                    null)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Failed to create workflow", "/manage/workflow"));
    }

    @GET
    @Path("/workflow/{workflowId}")
    public Response renderWorkflowDetailsScreen(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId) {
        return workflowStore.read(workflowId)
                .flatMap(workflow -> schemaStore.get(workflow.getSchemaId())
                        .map(schema -> new WorkflowDetailsView(user.getUserSession().getUser(), workflow, schema)))
                .map(ConductorServerUtils::render)
                .orElseThrow(() -> fail("Failed to find workflow " + workflowId, "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateWorkflow(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("titleTemplate") @NotEmpty @Length(max = 4096) final String titleTemplate,
            @FormParam("descriptionTemplate") @NotEmpty @Length(max = 4096) final String descriptionTemplate,
            @FormParam("subjectTemplate") @NotEmpty @Length(max = 4096) final String subjectTemplate) {
        return workflowStore.update(workflowId,
                                    workflow -> workflow.setDescription(description)
                                            .setTitleTemplate(template(titleTemplate))
                                            .setDescriptionTemplate(template(descriptionTemplate))
                                            .setSubjectIdTemplate(template(subjectTemplate)))
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Failed to update workflow " + workflowId, "/manage/workflow"));
    }

    @GET
    @Path("/workflow/{workflowId}/states/add")
    public Response createState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId) {
        return workflowManager.read(workflowId)
                .map(Workflow::getSchemaId)
                .flatMap(schemaStore::get)
                .map(Schema::getFields)
                .map(fields -> render(new WorkflowStateView(
                        user.getUserSession().getUser(),
                        workflowId,
                        null,
                        List.of(),
                        fields)))
                .orElseThrow(() -> fail("Failed to find workflow " + workflowId, "/manage/workflow"));
    }

    @GET
    @Path("/workflow/{workflowId}/states/{stateId}")
    public Response createState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("stateId") @NotEmpty @Length(max = 1024) final String stateId) {
        val workflow = workflowManager.read(workflowId);
        if (workflow.isEmpty()) {
            throw fail("No workflow found for: " + workflowId, "/");
        }
        val state = workflow.map(wf -> wf.getStates().get(stateId)).orElse(null);
        if (null == state) {
            throw fail("No state " + stateId + " found for workflow: " + workflowId,
                       "/manage/workflow/" + workflowId);
        }
        val fields = workflow
                .map(Workflow::getSchemaId)
                .flatMap(schemaStore::get)
                .map(Schema::getFields)
                .orElse(List.of());
        return render(new WorkflowStateView(user.getUserSession().getUser(),
                                            workflowId,
                                            state,
                                            List.of(),
                                            fields));
    }

    @POST
    @Path("/workflow/{workflowId}/states/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("stateName") @NotEmpty @Length(max = 45) final String stateName,
            @FormParam("stateDescription") @Length(max = 255) final String stateDescription,
            @FormParam("stateIsTerminal") @DefaultValue("false") final boolean stateIsTerminal,
            @FormParam("allowedActions") final List<String> allowedActions,
            @FormParam("editableFields") final List<String> editableFields,
            @FormParam("visibleFields") final List<String> visibleFields) {
        return workflowManager.createState(workflowId,
                                           stateName,
                                           stateDescription,
                                           stateIsTerminal,
                                           allowedActions,
                                           editableFields,
                                           visibleFields)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not add state " + stateName, "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/states/{stateId}/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("stateId") @NotEmpty @Length(max = 1024) final String stateId,
            @FormParam("stateDescription") @Length(max = 255) final String stateDescription,
            @FormParam("stateIsTerminal") @DefaultValue("false") final boolean stateIsTerminal,
            @FormParam("allowedActions") final List<String> allowedActions,
            @FormParam("editableFields") final List<String> editableFields,
            @FormParam("visibleFields") final List<String> visibleFields) {
        val workflow = workflowManager.read(workflowId);
        if (workflow.isEmpty()) {
            throw fail("No workflow found for: " + workflowId, "/");
        }
        val state = workflow.map(wf -> wf.getStates().get(stateId)).orElse(null);
        if (null == state) {
            throw fail("No state " + stateId + " found for workflow: " + workflowId,
                       "/manage/workflow/" + workflowId);
        }
        return workflowManager.updateState(workflowId,
                                           stateId,
                                           stateDescription,
                                           stateIsTerminal,
                                           allowedActions,
                                           editableFields,
                                           visibleFields)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not setup initial state state " + stateId + " for workflow " + workflowId,
                                        "/manage/workflow"));    }

    @POST
    @Path("/workflow/{workflowId}/states/{stateId}/initial")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response setupInitialStateForWorkflow(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("stateId") @NotEmpty @Length(max = 45) final String stateId) {
        return workflowManager.setupInitialStateForWorkflow(workflowId, stateId)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not setup initial state " + stateId + " for workflow " + workflowId,
                                        "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/states/{stateId}/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("stateId") @NotEmpty @Length(max = 45) final String stateId) {
        return workflowManager.deleteState(workflowId, stateId)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not delete state " + stateId + " for workflow " + workflowId,
                                        "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/transitions/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createTransition(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("fromState") @NotEmpty @Length(max = 45) final String fromState,
            @FormParam("toState") @Length(max = 45) final String toState,
            @FormParam("transitionType") final TicketStateTransition.TicketStateTransitionType transitionType,
            @FormParam("rule") @Length(max = 4096) final String rule,
            @FormParam("transitionAllowedActions") final List<String> allowedActions) {
        return workflowManager.createOrUpdateTransition(workflowId,
                                                        fromState,
                                                        toState,
                                                        transitionType,
                                                        transitionType == TicketStateTransition.TicketStateTransitionType.EVALUATED
                                                        ? new Rule(Rule.RuleType.HOPE, rule) : null,
                                                        allowedActions)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not add transition to workflow " + workflowId,
                                        "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/transitions/{transitionId}/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteTransition(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("transitionId") @NotEmpty @Length(max = 1024) final String transitionId) {
        return workflowStore.deleteTransition(workflowId, transitionId)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not delete transition from workflow " + workflowId,
                                        "/manage/workflow"));
    }

    private static io.appform.conductor.model.workflow.Template template(String templateValue) {
        return new io.appform.conductor.model.workflow.Template(io.appform.conductor.model.workflow.Template.Type.HANDLEBARS,
                                                                templateValue);
    }
}
