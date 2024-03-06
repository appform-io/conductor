/*
 * Copyright (c) 2024 santanu
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

import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.AttributeType;
import io.appform.conductor.model.attributes.definition.impl.*;
import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.server.attributes.definition.AttributeDefinitionStore;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.ui.views.manage.AttributeDefMgmtView;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.Constants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.stream.Collectors;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 *
 */
@Path("/ui/manage/attributes")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class ManageAttributeDefinitions {
    private final AttributeDefinitionStore attrDefStore;

    @GET
    @Path("/{scopeType}")
    public Response renderAttributeDefMgmtPage(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final AttributeScopeType scopeType) {
        return render(new AttributeDefMgmtView(user.getUserSession().getUser(),
                                               scopeType,
                                               attrDefStore.readAll(scopeType),
                                               null));
    }

    @POST
    @Path("/{scopeType}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_ATTRIBUTE_DEFINITIONS)
    public Response createAttributeDef(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final AttributeScopeType scopeType,
            @FormParam("name") @NotEmpty @Length(max = 45) final String name,
            @FormParam("displayName") @Length(max = 45) final String displayName,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("type") @NotNull final AttributeType type,
            @FormParam("stringMaxLength") @Min(1) @Max(255) final int stringMaxLength,
            @FormParam("stringRegex") final String stringRegex,
            @FormParam("choiceChoices") final String choiceChoices,
            @FormParam("choiceMulti") @DefaultValue("false") final boolean choiceMulti,
            @FormParam("numberMin") final double numberMin,
            @FormParam("numberMax") final double numberMax) {
        val id = ConductorServerUtils.readableId(scopeType.name(), name);
        return saveAttribute(scopeType,
                             name,
                             displayName,
                             description,
                             type,
                             stringMaxLength,
                             stringRegex,
                             choiceChoices,
                             choiceMulti,
                             numberMin,
                             numberMax,
                             id);
    }

    @GET
    @Path("/{scopeType}/{attrDefId}")
    public Response renderAttributeDefMgmtPage(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final AttributeScopeType scopeType,
            @PathParam("attrDefId") @NotEmpty @Length(max = 255) final String attrDefId) {
        return render(new AttributeDefMgmtView(user.getUserSession().getUser(),
                                               scopeType,
                                               attrDefStore.readAll(scopeType),
                                               attrDefStore.read(scopeType, attrDefId).orElse(null)));
    }

    @POST
    @Path("/{scopeType}/{attrDefId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.MANAGE_ATTRIBUTE_DEFINITIONS)
    public Response updateAttributeDef(
            @Auth final ConductorUser user,
            @PathParam("scopeType") @NotNull final AttributeScopeType scopeType,
            @PathParam("attrDefId") @NotEmpty @Length(max = 255) final String attrDefId,
            @FormParam("displayName") @Length(max = 45) final String displayName,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("stringMaxLength") @Min(1) @Max(255) final int stringMaxLength,
            @FormParam("stringRegex") final String stringRegex,
            @FormParam("choiceChoices") final String choiceChoices,
            @FormParam("choiceMulti") @DefaultValue("false") final boolean choiceMulti,
            @FormParam("numberMin") final double numberMin,
            @FormParam("numberMax") final double numberMax) {

        return attrDefStore.read(scopeType, attrDefId)
                .map(attr -> saveAttribute(scopeType,
                                           attr.getName(),
                                           displayName,
                                           description,
                                           attr.getType(),
                                           stringMaxLength,
                                           stringRegex,
                                           choiceChoices,
                                           choiceMulti,
                                           numberMin,
                                           numberMax,
                                           attr.getId()))
                .orElseThrow(() -> fail("No attribute found for scope " + scopeType.name()
                                                .toLowerCase() + " and id " + attrDefId,
                                        "/manage/attributes/" + scopeType));
    }

    private Response saveAttribute(
            AttributeScopeType scopeType,
            String name,
            String displayName,
            String description,
            AttributeType type,
            int stringMaxLength,
            String stringRegex,
            String choiceChoices,
            boolean choiceMulti,
            double numberMin,
            double numberMax,
            String id) {
        val def = switch (type) {
            case STRING -> new StringAttributeDefinition(id,
                                                         name,
                                                         displayName,
                                                         description,
                                                         null,
                                                         null,
                                                         stringMaxLength,
                                                         stringRegex);
            case CHOICE -> new ChoiceAttributeDefinition(id,
                                                         name,
                                                         displayName,
                                                         description,
                                                         null,
                                                         null,
                                                         Arrays.stream(choiceChoices.split(","))
                                                                 .map(String::trim)
                                                                 .collect(Collectors.toUnmodifiableSet()),
                                                         choiceMulti);
            case NUMBER -> new NumberAttributeDefinition(id,
                                                         name,
                                                         displayName,
                                                         description,
                                                         null,
                                                         null,
                                                         numberMax,
                                                         numberMin);
            case DATE -> new DateAttributeDefinition(id,
                                                     name,
                                                     displayName,
                                                     description,
                                                     null,
                                                     null);
            case LINK -> new LinkAttributeDefinition(id,
                                                     name,
                                                     displayName,
                                                     description,
                                                     null,
                                                     null);
        };
        return attrDefStore.save(scopeType, id, def)
                .map(d -> redirect("/manage/attributes/" + scopeType))
                .orElseThrow(() -> fail("Failed to add " + scopeType.name().toLowerCase() + " attribute definition",
                                        "/manage/attributes/" + scopeType));
    }

}
