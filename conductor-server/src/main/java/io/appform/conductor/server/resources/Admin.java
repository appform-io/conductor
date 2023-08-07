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

import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.auth.Role;
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.auth.RoleStore;
import io.appform.conductor.server.auth.UserRoleMappingStore;
import io.appform.conductor.server.ui.views.admin.RoleDetailsView;
import io.appform.conductor.server.ui.views.admin.RolesListView;
import io.appform.conductor.server.ui.views.admin.UserAdminView;
import io.appform.conductor.server.ui.views.user.UserSearchView;
import io.appform.conductor.server.usermanagement.UserLifecycleManager;
import io.appform.conductor.server.usermanagement.UserStore;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static io.appform.conductor.server.utils.ConductorServerUtils.lowerSnake;

/**
 * Administration ui
 */
@Path("/ui/admin")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
public class Admin {
    private final RoleStore roleStore;
    private final UserStore userStore;
    private final UserRoleMappingStore roleMappingStore;
    private final UserLifecycleManager userLifecycleManager;

    @GET
    @Path("/roles")
    public Response renderRolesList(@Auth ConductorUser user) {
        return Response.ok(new RolesListView(user.getUserSession().getUser(), roleStore.list())).build();
    }

    @GET
    @Path("/roles/create")
    public Response renderCreateRoleView(@Auth ConductorUser user) {
        return Response.ok(new RoleDetailsView(user.getUserSession().getUser(),
                                               null)).build();
    }

    @POST
    @Path("/roles/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createRole(
            @FormParam("name") @Length(min = 1, max = 45) final String name,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("permissions") @NotEmpty List<Permission> permissions) {
        return roleStore.create(lowerSnake(name), name, description, Set.copyOf(permissions))
                .map(role -> Response.seeOther(URI.create("/admin/roles")).build())
                .orElse(Response.seeOther(URI.create("/")).build());
    }

    @GET
    @Path("/roles/update/{roleId}")
    public Response renderRoleDetails(
            @Auth ConductorUser user,
            @PathParam("roleId") @NotEmpty final String roleId) {
        return roleStore.read(roleId)
                .map(role -> {
                    val permissions = new EnumMap<Permission, Boolean>(Permission.class);
                    Arrays.stream(Permission.values())
                            .forEach(permission -> permissions.put(permission,
                                                                   role.getPermissions().contains(permission)));
                    return Response.ok(new RoleDetailsView(user.getUserSession().getUser(),
                                                           role)).build();
                })
                .orElse(Response.seeOther(URI.create("/")).build());

    }

    @POST
    @Path("/roles/update/{roleId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateRole(
            @PathParam("roleId") @Length(min = 1, max = 45) final String roleId,
            @FormParam("description") @Length(max = 255) final String description,
            @FormParam("permissions") @NotEmpty Set<Permission> permissions) {
        return roleStore.update(roleId,
                                role -> new Role(roleId,
                                                 role.getName(),
                                                 description,
                                                 permissions,
                                                 role.getCreated(),
                                                 role.getUpdated()))
                .map(role -> Response.seeOther(URI.create("/admin/roles")).build())
                .orElse(Response.seeOther(URI.create("/admin/roles")).build());
    }

    @POST
    @Path("/roles/delete/{roleId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateRole(
            @PathParam("roleId") @Length(min = 1, max = 45) final String roleId) {
        roleStore.delete(roleId);
        return Response.seeOther(URI.create("/admin/roles")).build();
    }

    @GET
    @Path("/users/search")
    public Response renderUserSearchScreen(@Auth ConductorUser user) {
        return Response.ok(new UserSearchView(user.getUserSession().getUser())).build();
    }

    @POST
    @Path("/users/email")
    public Response searchUserByEmail(
            @Auth ConductorUser user,
            @FormParam("email") @NotEmpty @Email final String email) {
        return userStore.getByEmail(email)
                .map(userSummary -> Response.seeOther(URI.create("/admin/users/" + userSummary.getId())).build())
                .orElse(Response.seeOther(URI.create("/admin/users/search")).build());
    }

    @GET
    @Path("/users/{userId}")
    public Response renderUserAdminPage(
            @Auth ConductorUser user,
            @PathParam("userId") @NotEmpty final String userId) {
        return userLifecycleManager.userDetails(userId)
                .map(userDetails -> Response.ok(new UserAdminView(user.getUserSession().getUser(), userDetails,
                                                                  roleStore.list())).build())
                .orElse(Response.seeOther(URI.create("/admin/users/search")).build());
    }

    @POST
    @Path("/users/{userId}/state")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response changeUserState(
            @Auth ConductorUser user,
            @PathParam("userId") @NotEmpty final String userId,
            @FormParam("state") @NotNull UserState state) {
        return userStore.updateState(userId, state)
                .map(userSummary -> Response.seeOther(URI.create("/admin/users/" + userSummary.getId())).build())
                .orElse(Response.seeOther(URI.create("/admin/users/search")).build());
    }

    @POST
    @Path("/users/{userId}/role")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response assignUserRole(
            @Auth ConductorUser user,
            @PathParam("userId") @NotEmpty final String userId,
            @FormParam("roleId") @NotEmpty String roleId) {
        if(roleMappingStore.assignRoleToUser(userId, roleId)) {
            return Response.seeOther(URI.create("/admin/users/" + userId)).build();
        }
        return Response.seeOther(URI.create("/admin/users/search")).build();
    }
}
