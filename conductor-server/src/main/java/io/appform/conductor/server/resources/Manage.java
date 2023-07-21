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
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.ui.views.admin.SchemaCreateView;
import io.appform.conductor.server.ui.views.admin.SchemaDetailsView;
import io.appform.conductor.server.ui.views.admin.SchemaListView;
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
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

import static io.appform.conductor.server.utils.ConductorServerUtils.upperSnake;

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

    @GET
    @Path("/schema")
    public Response renderSchemaList(@Auth ConductorUser user) {
        return Response.ok(new SchemaListView(user.getUserSession().getUser(), schemaStore.list()))
                .build();
    }

    @GET
    @Path("/schema/create")
    public Response renderSchemaCreate(@Auth ConductorUser user) {
        return Response.ok(new SchemaCreateView(user.getUserSession().getUser()))
                .build();
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
                .map(schemaSummary -> Response.seeOther(URI.create("/manage/schema/" + schemaSummary.getId())).build())
                .orElse(Response.seeOther(URI.create("/manage/schema")).build());
    }


    @POST
    @Path("/schema/update/{schemaId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateSchemaDescription(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId,
            @FormParam("description") @Length(max = 255) final String description) {
        return schemaStore.updateDescription(schemaId, description)
                .map(schemaSummary -> Response.seeOther(URI.create("/manage/schema/" + schemaSummary.getId())).build())
                .orElse(Response.seeOther(URI.create("/manage/schema")).build());
    }

    @GET
    @Path("/schema/{schemaId}")
    public Response renderSchemaDetails(
            @Auth ConductorUser user,
            @PathParam("schemaId") @NotEmpty @Length(max = 45) final String schemaId) {
        return schemaStore.get(schemaId)
                .map(schema -> Response.ok(new SchemaDetailsView(user.getUserSession().getUser(), schema)).build())
                .orElse(Response.seeOther(URI.create("/manage/schema")).build());
    }


    @POST
    @Path("/schema/{schemaId}/fields/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateSchemaDescription(
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
                .filter(Objects::nonNull)
                .map(f -> Response.seeOther(URI.create("/manage/schema/" + schemaId)).build())
                .orElse(Response.seeOther(URI.create("/manage/schema")).build());
    }
}
