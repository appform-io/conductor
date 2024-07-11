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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.schema.*;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.model.usermgmt.Skill;
import io.appform.conductor.model.workflow.*;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.config.AuthConfig;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.ui.views.manage.*;
import io.appform.conductor.server.usermanagement.UserLifecycleManager;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.workflowmanagement.WorkflowManager;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private final UserLifecycleManager userLifecycleManager;
    private final ActionStore actionStore;
    private final AuthConfig authConfig;
    private final ObjectMapper mapper;

    @GET
    @Path("/schema")
    public Response renderSchemaList(@Auth ConductorUser user) {
        return render(new SchemaListView(user.getUserSession().getUser(), schemaStore.list()));
    }

    @GET
    @Path("/schema/create")
    @RolesAllowed(Permission.Values.MANAGE_SCHEMA)
    public Response renderSchemaCreate(@Auth ConductorUser user) {
        return render(new SchemaView(user.getUserSession().getUser(), null, null));
    }

    @POST
    @Path("/schema/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_SCHEMA)
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
    @RolesAllowed(Permission.Values.MANAGE_SCHEMA)
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
        return schemaStore.read(schemaId)
                .map(schema -> render(new SchemaView(user.getUserSession().getUser(), schema, null)))
                .orElseThrow(() -> fail("Failed to find schema " + schemaId, "/manage/schema"));
    }


    @GET
    @Path("/schema/{schemaId}/fields/{fieldId}")
    public Response renderSchemaFieldDetails(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @PathParam("fieldId") @NotEmpty @Length(max = 91) final String fieldId) {
        return schemaStore.read(schemaId)
                .map(schema -> render(new SchemaView(user.getUserSession().getUser(),
                                                     schema,
                                                     schema.getFields()
                                                             .stream()
                                                             .filter(fieldSchema -> fieldSchema.getId().equals(fieldId))
                                                             .findFirst()
                                                             .orElse(null))))
                .orElseThrow(() -> fail("Failed to find schema " + schemaId, "/manage/schema"));
    }

    @POST
    @Path("/schema/{schemaId}/fields/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_SCHEMA)
    public Response addField(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @FormParam("fieldName") @NotEmpty @Length(max = 45) final String fieldName,
            @FormParam("fieldDisplayName") @Length(max = 45) final String fieldDisplayName,
            @FormParam("fieldDescription") @Length(max = 255) final String fieldDescription,
            @FormParam("fieldType") @NotNull final FieldType fieldType,
            @FormParam("fieldStringMaxLength") @Min(1) @Max(255) final int fieldStringMaxLength,
            @FormParam("fieldStringRegex") final String fieldStringRegex,
            @FormParam("fieldChoiceChoices") final String fieldChoiceChoices,
            @FormParam("fieldChoiceMulti") @DefaultValue("false") final boolean fieldChoiceMulti,
            @FormParam("fieldNumberMin") final double fieldNumberMin,
            @FormParam("fieldNumberMax") final double fieldNumberMax) {
        //TODO::DEFAULT VALUE TO BE PREFILLED
        val displayName = Strings.isNullOrEmpty(fieldDisplayName) ? fieldName : fieldDisplayName;
        val fs = switch (fieldType) {
            case STRING -> StringFieldSchema.builder()
                    .name(fieldName)
                    .displayName(displayName)
                    .description(fieldDescription)
                    .maxLength(fieldStringMaxLength)
                    .matchPattern(fieldStringRegex)
                    .build();
            case CHOICE -> ChoiceFieldSchema.builder()
                    .name(fieldName)
                    .displayName(displayName)
                    .description(fieldDescription)
                    .allowMultiple(fieldChoiceMulti)
                    .choices(Arrays.stream(fieldChoiceChoices.split(","))
                                     .map(choice -> new ChoiceFieldSchema.Option(upperSnake(choice), choice))
                                     .toList())
                    .build();
            case BOOLEAN -> BooleanFieldSchema.builder()
                    .name(fieldName)
                    .displayName(displayName)
                    .description(fieldDescription)
                    .build();
            case NUMBER -> NumberFieldSchema.builder()
                    .name(fieldName)
                    .displayName(displayName)
                    .description(fieldDescription)
                    .min(fieldNumberMin)
                    .max(fieldNumberMax)
                    .build();
            case LOCATION -> LocationFieldSchema.builder()
                    .name(fieldName)
                    .displayName(displayName)
                    .description(fieldDescription)
                    .build();
            case DATE -> DateFieldSchema.builder()
                    .name(fieldName)
                    .displayName(displayName)
                    .description(fieldDescription)
                    .build();
        };
        return schemaStore.addField(schemaId, ConductorServerUtils.readableId(schemaId, fieldName), fs)
                .map(f -> redirect("/manage/schema/" + schemaId))
                .orElseThrow(() -> fail("Failed to add field to schema " + schemaId, "/manage/schema"));
    }


    @POST
    @Path("/schema/{schemaId}/fields/{fieldId}/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_SCHEMA)
    public Response updateField(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @PathParam("fieldId") @NotEmpty @Length(max = 45) final String fieldId,
            @FormParam("fieldDisplayName") @Length(max = 45) final String fieldDisplayName,
            @FormParam("fieldDescription") @Length(max = 255) final String fieldDescription,
            @FormParam("fieldStringMaxLength") @Min(1) @Max(255) final int fieldStringMaxLength,
            @FormParam("fieldStringRegex") final String fieldStringRegex,
            @FormParam("fieldChoiceChoices") final String fieldChoiceChoices,
            @FormParam("fieldChoiceMulti") @DefaultValue("false") final boolean fieldChoiceMulti,
            @FormParam("fieldNumberMin") final double fieldNumberMin,
            @FormParam("fieldNumberMax") final double fieldNumberMax) {
        //TODO::DEFAULT VALUE TO BE PREFILLED
        return schemaStore.getField(schemaId, fieldId)
                .map(field -> field.setDisplayName(fieldDisplayName)
                        .setDescription(fieldDescription))
                .map(field -> field.accept(new FieldSchemaVisitor<FieldSchema>() {
                    @Override
                    public FieldSchema visit(StringFieldSchema stringField) {
                        return stringField
                                .setMaxLength(fieldStringMaxLength)
                                .setMatchPattern(fieldStringRegex);
                    }

                    @Override
                    public FieldSchema visit(NumberFieldSchema numberField) {
                        return numberField
                                .setMin(fieldNumberMin)
                                .setMax(fieldNumberMax);
                    }

                    @Override
                    public FieldSchema visit(BooleanFieldSchema booleanField) {
                        return booleanField;
                    }

                    @Override
                    public FieldSchema visit(LocationFieldSchema locationField) {
                        return locationField;
                    }

                    @Override
                    public FieldSchema visit(DateFieldSchema dateField) {
                        return dateField;
                    }

                    @Override
                    public FieldSchema visit(ChoiceFieldSchema choiceField) {
                        return choiceField
                                .setChoices(Arrays.stream(fieldChoiceChoices.split(","))
                                                    .map(choice -> new ChoiceFieldSchema.Option(upperSnake(choice),
                                                                                                choice))
                                                    .toList())
                                .setAllowMultiple(fieldChoiceMulti);
                    }
                }))
                .map(field -> schemaStore.updateField(schemaId, fieldId, field))
                .map(f -> redirect("/manage/schema/" + schemaId))
                .orElseThrow(() -> fail("Failed to update field " + schemaId + "/" + fieldId, "/manage/schema"));
    }

    @POST
    @Path("/schema/{schemaId}/fields/{fieldId}/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_SCHEMA)
    public Response deleteField(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @PathParam("fieldId") @NotEmpty @Length(max = 45) final String fieldId) {
        if (schemaStore.deleteField(schemaId, fieldId)) {
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
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
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
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
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
                .flatMap(wf -> workflowStore.updateWorkflowState(wf.getId(), WorkflowState.ACTIVE)) //TODO: ACTIVATION FLOW
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Failed to create workflow", "/manage/workflow"));
    }

    @GET
    @Path("/workflow/{workflowId}")
    public Response renderWorkflowDetailsScreen(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId) {
        return workflowStore.read(workflowId)
                .flatMap(workflow -> schemaStore.read(workflow.getSchemaId())
                        .map(schema -> new WorkflowDetailsView(
                                user.getUserSession().getUser(),
                                workflow,
                                schema,
                                null,
                                relevantActionsList(workflowId),
                                workflowActions(workflow))))
                .map(ConductorServerUtils::render)
                .orElseThrow(() -> fail("Failed to find workflow " + workflowId, "/manage/workflow"));
    }

    @GET
    @Path("/workflow/{workflowId}/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportWorkflow(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId) {
        val workflow = workflowManager.workflowDetails(workflowId);
        val fileName = String.format("%s-%s.json", workflowId,
                                     new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()));
        return Response.ok((StreamingOutput)output -> output.write(mapper.writerWithDefaultPrettyPrinter()
                             .writeValueAsString(workflow)
                             .getBytes(StandardCharsets.UTF_8)))
                .header("content-disposition","attachment; filename = " + fileName)
                .build();
    }

    @GET
    @Path("/workflow/import")
    public Response renderImportScreen(@Auth ConductorUser user) {
        return render(new WorkflowImportView(user.getUserSession().getUser(), null));
    }

    @POST
    @Path("/workflow/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @SneakyThrows
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response uploadFile(
            @Auth ConductorUser user,
            @FormDataParam("forceOverwriteActions") @DefaultValue("false") boolean forceOverwriteActions,
            @FormDataParam("forceOverwriteTasks") @DefaultValue("false") boolean forceOverwriteTasks,
            @FormDataParam("workflowFile") InputStream input,
            @FormDataParam("workflowFile") FormDataContentDisposition fileDetail) {
        val workflow = mapper.readValue(input, WorkflowDetails.class);
        return render(new WorkflowImportView(user.getUserSession().getUser(),
                                             workflowManager.importWorkflow(workflow,
                                                                            forceOverwriteActions,
                                                                            forceOverwriteTasks)));
    }

    @POST
    @Path("/workflow/{workflowId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response updateWorkflowDescription(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("description") @Length(max = 255) final String description) {
        return workflowManager.updateDescription(workflowId, description)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Failed to update workflow " + workflowId, "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/dynamic")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response updateWorkflowDynamicTemplates(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("titleTemplate") @NotEmpty @Length(max = 4096) final String titleTemplate,
            @FormParam("descriptionTemplate") @NotEmpty @Length(max = 4096) final String descriptionTemplate,
            @FormParam("subjectTemplate") @NotEmpty @Length(max = 4096) final String subjectTemplate) {
        return workflowManager.updateTemplates(workflowId,
                                               template(titleTemplate),
                                               template(descriptionTemplate),
                                               template(subjectTemplate))
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Failed to update workflow " + workflowId, "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/selectionrules/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response addWorkflowSelectionRule(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("selectionRule") @NotEmpty @Length(max = 4096) final String selectionRule) {
        return workflowManager.addSelectionRule(workflowId, new Rule(Rule.RuleType.HOPE, selectionRule))
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Failed to update workflow " + workflowId, "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/selectionrules/{ruleId}/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response deleteWorkflowSelectionRule(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("ruleId") @NotEmpty @Length(max = 45) final String ruleId) {
        return workflowManager.deleteSelectionRule(workflowId, ruleId)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Failed to update workflow " + workflowId, "/manage/workflow"));
    }

    @GET
    @Path("/workflow/{workflowId}/states/add")
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response createState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId) {
        val workflow = workflowManager.read(workflowId);
        return workflow
                .map(Workflow::getSchemaId)
                .flatMap(schemaStore::read)
                .map(Schema::getFields)
                .map(fields -> render(new WorkflowStateView(
                        user.getUserSession().getUser(),
                        workflowId,
                        null,
                        actionStore.listActionsForIds(workflow.map(Workflow::getAvailableActions).orElse(List.of())),
                        actionStore.listActionsForScopes(List.of(Scope.GLOBAL)),
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
                .flatMap(schemaStore::read)
                .map(Schema::getFields)
                .orElse(List.of());
        return render(new WorkflowStateView(user.getUserSession().getUser(),
                                            workflowId,
                                            state,
                                            actionStore.listActionsForIds(workflow.map(Workflow::getAvailableActions)
                                                                                  .orElse(List.of())),
                                            actionStore.listActionsForScopes(Set.of(Scope.GLOBAL,
                                                                                    Scope.create(Scope.ScopeType.STATE,
                                                                                                 stateId))),
                                            fields));
    }

    @POST
    @Path("/workflow/{workflowId}/states/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response createState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("stateName") @NotEmpty @Length(max = 45) final String stateName,
            @FormParam("stateDescription") @Length(max = 255) final String stateDescription,
            @FormParam("stateIsTerminal") @DefaultValue("false") final boolean stateIsTerminal,
            @FormParam("allowedActions") final List<String> allowedActions,
            @FormParam("editableFields") final List<String> editableFields,
            @FormParam("visibleFields") final List<String> visibleFields,
            @FormParam("requiredFields") final List<String> requiredFields,
            @FormParam("visibleActions") final List<String> visibleActions) {
        return workflowManager.createState(workflowId,
                                           stateName,
                                           stateDescription,
                                           stateIsTerminal,
                                           allowedActions,
                                           editableFields,
                                           visibleFields,
                                           requiredFields,
                                           visibleActions)
                .map(pair -> redirect("/manage/workflow/" + pair.getFirst().getId() + "/states/" + pair.getSecond()))
                .orElseThrow(() -> fail("Could not add state " + stateName, "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/states/{stateId}/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response updateState(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("stateId") @NotEmpty @Length(max = 1024) final String stateId,
            @FormParam("stateDescription") @Length(max = 255) final String stateDescription,
            @FormParam("stateIsTerminal") @DefaultValue("false") final boolean stateIsTerminal,
            @FormParam("allowedActions") final List<String> allowedActions,
            @FormParam("editableFields") final List<String> editableFields,
            @FormParam("visibleFields") final List<String> visibleFields,
            @FormParam("requiredFields") final List<String> requiredFields,
            @FormParam("visibleActions") final List<String> visibleActions) {
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
                                           visibleFields,
                                           requiredFields,
                                           visibleActions)
                .map(wf -> redirect("/manage/workflow/" + wf.getId() + "/states/" + stateId))
                .orElseThrow(() -> fail("Could not setup initial state state " + stateId + " for workflow " + workflowId,
                                        "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/states/{stateId}/initial")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
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
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
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
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
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

    @GET
    @Path("/workflow/{workflowId}/transitions/{stateTransitionId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response renderTransition(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("stateTransitionId") @NotEmpty @Length(max = 255) final String stateTransitionId) {
        return workflowStore.read(workflowId)
                .flatMap(workflow -> schemaStore.read(workflow.getSchemaId())
                        .map(schema -> new WorkflowDetailsView(
                                user.getUserSession().getUser(),
                                workflow,
                                schema,
                                workflow.getTicketStateTransitions()
                                        .values()
                                        .stream()
                                        .flatMap(List::stream)
                                        .filter(ticketStateTransition -> ticketStateTransition.getId()
                                                .equals(stateTransitionId))
                                        .findFirst()
                                        .orElse(null),
                                relevantActionsList(workflowId),
                                workflowActions(workflow))))
                .map(ConductorServerUtils::render)
                .orElseThrow(() -> fail("Failed to find workflow " + workflowId, "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/actions/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response addAvailableAction(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @FormParam("availableAction") @NotEmpty @Length(max = 45) final String availableAction) {
        return workflowManager.addAvailableAction(workflowId, availableAction)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not add action to workflow " + workflowId,
                                        "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/actions/{actionId}/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response removeAvailableAction(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("actionId") @NotEmpty @Length(max = 45) final String actionId) {
        return workflowManager.removeAvailableAction(workflowId, actionId)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not add action to workflow " + workflowId,
                                        "/manage/workflow"));
    }

    @POST
    @Path("/workflow/{workflowId}/transitions/{stateTransitionId}/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response updateTransition(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("stateTransitionId") @NotEmpty @Length(max = 255) final String stateTransitionId,
            @FormParam("transitionType") final TicketStateTransition.TicketStateTransitionType transitionType,
            @FormParam("rule") @Length(max = 4096) final String rule,
            @FormParam("transitionAllowedActions") final List<String> allowedActions) {
        return workflowManager.updateTransition(workflowId,
                                                stateTransitionId,
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
    @RolesAllowed(Permission.Values.MANAGE_WORKFLOW)
    public Response deleteTransition(
            @Auth ConductorUser user,
            @PathParam("workflowId") @NotEmpty @Length(max = 45) final String workflowId,
            @PathParam("transitionId") @NotEmpty @Length(max = 1024) final String transitionId) {
        return workflowStore.deleteTransition(workflowId, transitionId)
                .map(wf -> redirect("/manage/workflow/" + wf.getId()))
                .orElseThrow(() -> fail("Could not delete transition from workflow " + workflowId,
                                        "/manage/workflow"));
    }

    @GET
    @Path("/groups")
    public Response renderGroupList(@Auth ConductorUser user) {
        return render(new GroupListView(user.getUserSession().getUser(), userLifecycleManager.listGroups(), null,
                                        userLifecycleManager.listSkillValues()));
    }

    @POST
    @Path("/groups")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response createGroups(
            @Auth ConductorUser user,
            @FormParam("name") @NotEmpty @Length(max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("type") @DefaultValue("MANUALLY_ASSIGNED") @NotNull final GroupType type,
            @FormParam("skillValueId") @Size(max = 8) final Set<String> skillValueId) {
        return userLifecycleManager.createGroup(name, description, type, skillValueId)
                .map(group -> redirect("/manage/groups/" + group.getId()))
                .orElseThrow(() -> fail("Could not create group", "/manage/groups"));
    }

    @GET
    @Path("/groups/{groupId}")
    public Response createGroup(
            @Auth ConductorUser user,
            @PathParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        return userLifecycleManager.readGroup(groupId)
                .map(group -> render(new GroupListView(user.getUserSession().getUser(),
                                                       userLifecycleManager.listGroups(),
                                                       group,
                                                       userLifecycleManager.listSkillValues())))
                .orElseThrow(() -> fail("No such group exists", "/manage/groups"));
    }

    @POST
    @Path("/groups/{groupId}/update")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response updateGroup(
            @Auth ConductorUser user,
            @PathParam("groupId") @NotEmpty @Length(max = 45) final String groupId,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("type") @DefaultValue("MANUALLY_ASSIGNED") @NotNull final GroupType type,
            @FormParam("skillValueId") @Size(max = 8) final Set<String> skillValueId) {
        return userLifecycleManager.updateGroup(groupId,
                                                description,
                                                type,
                                                skillValueId)
                .map(group -> redirect("/manage/groups/" + group.getId()))
                .orElseThrow(() -> fail("Could not update group", "/manage/groups"));
    }


    @POST
    @Path("/groups/{groupId}/delete")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response deleteGroup(
            @Auth ConductorUser user,
            @PathParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        val status = userLifecycleManager.deleteGroup(groupId);
        if(status) {
            return redirect("/manage/groups/");
        }
        throw fail("Could not create group", "/manage/groups");
    }

    @POST
    @Path("/users/{userId}/groups/add")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response addUserToGroup(
            @Auth final ConductorUser user,
            @PathParam("userId") @NotEmpty @Length(max = 45) final String userId,
            @FormParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        if (!authConfig.isDisableRoleCheck() && user.getUserSession().getUser().getSummary().getId().equals(userId)) {
            throw fail(
                    "Cannot add yourself to a group. Please ask someone with MANAGE_GROUPS permission to do this for " +
                            "you.",
                    "/admin/users/" + userId);
        }
        if (userLifecycleManager.addUserToGroup(groupId, userId)) {
            return redirect("/admin/users/" + userId);
        }
        throw fail("Could not add user to group", "/admin/users/" + userId);
    }

    @POST
    @Path("/users/{userId}/groups/{groupId}/remove")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response removeUserFromGroup(
            @Auth final ConductorUser user,
            @PathParam("userId") @NotEmpty @Length(max = 45) final String userId,
            @PathParam("groupId") @NotEmpty @Length(max = 45) final String groupId) {
        if (userLifecycleManager.removeUserFromGroup(groupId, userId)) {
            return redirect("/admin/users/" + userId);
        }
        throw fail("Could not remove user from group", "/admin/users/" + userId);
    }

    @POST
    @Path("/users/{userId}/skills/add")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response addUserSkill(
            @Auth final ConductorUser user,
            @PathParam("userId") @NotEmpty @Length(max = 45) final String userId,
            @FormParam("skillValueId") @NotEmpty @Length(max = 91) final String skillValueId) {
        val parts = skillValueId.split("/");
        if (parts.length == 2 && userLifecycleManager.addUserSkill(userId, new Skill(parts[0], parts[1]))) {
            return redirect("/admin/users/" + userId);
        }
        throw fail("Could not add skill to user", "/admin/users/" + userId);
    }

    @POST
    @Path("/users/{userId}/skills/{skillId}/{skillValueId}/remove")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response removeUserSkill(
            @Auth final ConductorUser user,
            @PathParam("userId") @NotEmpty @Length(max = 45) final String userId,
            @PathParam("skillId") @NotEmpty @Length(max = 45) final String skillId,
            @PathParam("skillValueId") @NotEmpty @Length(max = 45) final String skillValueId) {
        if (userLifecycleManager.removeUserSkill(userId, new Skill(skillId, skillValueId))) {
            return redirect("/admin/users/" + userId);
        }
        throw fail("Could not remove skill from user", "/admin/users/" + userId);
    }

    @GET
    @Path("/skills")
    public Response listSkills(
            @Auth final ConductorUser user) {
        return render(new SkillListView(user.getUserSession().getUser(),
                                        userLifecycleManager.listSkillDefinitions(),
                                        null));
    }

    @GET
    @Path("/skills/{skillId}")
    public Response showSkill(
            @Auth final ConductorUser user,
            @PathParam("skillId") @NotEmpty @Length(max = 45) final String skillId) {
        val skill = userLifecycleManager.getSkill(skillId).orElse(null);
        if (null != skill) {
            return render(new SkillListView(user.getUserSession().getUser(),
                                            userLifecycleManager.listSkillDefinitions(),
                                            skill));
        }
        throw fail("No skill found for id: " + skillId, "/manage/skills");
    }

    @POST
    @Path("/skills")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response updateSkill(
            @Auth final ConductorUser user,
            @FormParam("name") @NotEmpty @Length(max = 45) final String name) {
        return userLifecycleManager.createSkill(name)
                .map(skill -> redirect("/manage/skills/" + skill.getId()))
                .orElseThrow(() -> fail("Could not create skill", "/manage/skills"));
    }

    @POST
    @Path("/skills/{skillId}")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response updateSkill(
            @Auth final ConductorUser user,
            @PathParam("skillId") @NotEmpty @Length(max = 45) final String skillId,
            @FormParam("name") @NotEmpty @Length(max = 45) final String name) {
        return userLifecycleManager.updateSkillDefinition(skillId, name)
                .map(skill -> redirect("/manage/skills/" + skill.getId()))
                .orElseThrow(() -> fail("Could not create skill", "/manage/skills"));
    }

    @POST
    @Path("/skills/{skillId}/delete")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response deleteSkill(
            @Auth final ConductorUser user,
            @PathParam("skillId") @NotEmpty @Length(max = 45) final String skillId) {
        if (userLifecycleManager.deleteSkillDefinition(skillId)) {
            return redirect("/manage/skills/");
        }
        throw fail("Could not delete skill " + skillId, "/manage/skills");
    }

    @POST
    @Path("/skills/{skillId}/values")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response createSkillValue(
            @Auth final ConductorUser user,
            @PathParam("skillId") @NotEmpty @Length(max = 45) final String skillId,
            @FormParam("value") @NotEmpty @Length(max = 45) final String value) {
        return userLifecycleManager.addSkillValue(skillId, value)
                .map(skill -> redirect("/manage/skills/" + skill.getId()))
                .orElseThrow(() -> fail("Could not create skill", "/manage/skills"));
    }

    @POST
    @Path("/skills/{skillId}/values/{skillValueId}/delete")
    @RolesAllowed(Permission.Values.MANAGE_GROUPS)
    public Response deleteSkillValue(
            @Auth final ConductorUser user,
            @PathParam("skillId") @NotEmpty @Length(max = 45) final String skillId,
            @PathParam("skillValueId") @NotEmpty @Length(max = 45) final String skillValueId) {
        return userLifecycleManager.removeSkillValue(skillId, skillValueId)
                .map(skill -> redirect("/manage/skills/" + skill.getId()))
                .orElseThrow(() -> fail("Could not create skill", "/manage/skills"));
    }

    private List<Action> workflowActions(Workflow workflow) {
        return actionStore.listActionsForIds(workflow.getAvailableActions());
    }

    private List<Action> relevantActionsList(String workflowId) {
        return actionStore.listActionsForScopes(List.of(Scope.GLOBAL,
                                                        Scope.create(Scope.ScopeType.WORKFLOW,
                                                                     workflowId)));
    }

    private static io.appform.conductor.model.workflow.Template template(String templateValue) {
        return new io.appform.conductor.model.workflow.Template(io.appform.conductor.model.workflow.Template.Type.STRING_SUBSTITUTION,
                                                                templateValue);
    }

    private static Rule createAssignmentRule(GroupType type, List<String> skillValueId) {
        return switch (type) {
            case MANUALLY_ASSIGNED, SYSTEM_RESERVED -> null;
            case AUTOMATICALLY_ASSIGNED -> new Rule(Rule.RuleType.HOPE,
                                                    String.format("arr.contains_all([%s], '/skills') == true",
                                                                  String.join(",",
                                                                              skillValueId.stream()
                                                                                      .map(id -> "'" + id + "'")
                                                                                      .toList())));
        };
    }

}
