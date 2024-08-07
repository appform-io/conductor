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
import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.server.attributes.values.AttributeManager;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.auth.RoleStore;
import io.appform.conductor.server.auth.UserRoleMappingStore;
import io.appform.conductor.server.config.AuthConfig;
import io.appform.conductor.server.skillmanagement.SkillStore;
import io.appform.conductor.server.ui.views.admin.RolesListView;
import io.appform.conductor.server.ui.views.admin.UserAdminView;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.SessionStore;
import io.appform.conductor.server.usermanagement.UserLifecycleManager;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.utils.Constants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.ManualErrorHandling;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 * Administration ui
 */
@Path("/ui/admin")
@Template
@Produces(MediaType.TEXT_HTML)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@PermitAll
@ManualErrorHandling
@Slf4j
public class Admin {
    private static final String ROLES_LIST_PATH = "/admin/roles";
    private static final String USER_SEARCH_PATH = "/admin/users/search";

    private final RoleStore roleStore;
    private final UserStore userStore;
    private final GroupStore groupStore;
    private final SkillStore skillStore;
    private final UserRoleMappingStore roleMappingStore;
    private final UserLifecycleManager userLifecycleManager;
    private final AuthConfig authConfig;
    private final AttributeManager attributeManager;
    private final SessionStore sessionStore;

    @GET
    @Path("/roles")
    public Response renderRolesList(@Auth ConductorUser user) {
        return render(new RolesListView(user.getUserSession().getUser(), roleStore.list(), null));
    }

    @GET
    @Path("/roles/create")
    @RolesAllowed(Permission.Values.ADMIN)
    public Response renderCreateRoleView(@Auth ConductorUser user) {
        return redirect(ROLES_LIST_PATH);
    }

    @POST
    @Path("/roles/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response createRole(
            @FormParam("name") @Length(min = 1, max = Constants.MAX_ROLE_ID_LENGTH) final String name,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("permissions") @NotEmpty List<Permission> permissions) {
        return roleStore.create(lowerSnake(name), name, description, Set.copyOf(permissions))
                .map(role -> redirect(ROLES_LIST_PATH))
                .orElseThrow(() -> fail("Failed to create role", ROLES_LIST_PATH));
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
                    return render(new RolesListView(user.getUserSession().getUser(),
                                                    roleStore.list(),
                                                    role));
                })
                .orElseThrow(() -> fail("Could not find role with id: " + roleId, ROLES_LIST_PATH));

    }

    @POST
    @Path("/roles/update/{roleId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response updateRole(
            @PathParam("roleId") @Length(min = 1, max = 45) final String roleId,
            @FormParam("description") @Length(max = Constants.MAX_DESCRIPTION_LENGTH) final String description,
            @FormParam("permissions") @NotEmpty Set<Permission> permissions) {
        return roleStore.update(roleId,
                                role -> role.withDescription(description)
                                        .withPermissions(permissions))
                .map(role -> redirect(ROLES_LIST_PATH))
                .orElseThrow(() -> fail("Could not update role: " + roleId, ROLES_LIST_PATH));
    }

    @POST
    @Path("/roles/delete/{roleId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response updateRole(
            @PathParam("roleId") @Length(min = 1, max = 45) final String roleId) {
        if (roleStore.delete(roleId)) {
            return redirect(ROLES_LIST_PATH);
        }
        throw fail("Error deleting role: " + roleId, ROLES_LIST_PATH);
    }

    @GET
    @Path("/users/search")
    public Response renderUserSearchScreen(@Auth ConductorUser user) {
        return render(new UserAdminView(user.getUserSession().getUser(),
                                        null,
                                        List.of(),
                                        List.of(),
                                        List.of(),
                                        List.of(),
                                        List.of()));
    }

    @GET
    @Path("/users/email")
    public Response redirectToSearch() {
        return redirect("/admin/users/search");
    }

    @POST
    @Path("/users/email")
    public Response searchUserByEmail(
            @Auth ConductorUser user,
            @FormParam("email") @NotEmpty @Email final String email) {
        return userStore.getByEmail(email)
                .map(userSummary -> redirect("/admin/users/" + userSummary.getId()))
                .orElseThrow(() -> fail("No user found for " + email, USER_SEARCH_PATH));
    }

    @POST
    @Path("/users/userid")
    public Response searchUserByUserId(
            @Auth ConductorUser user,
            @FormParam("searchUserId") @NotEmpty @Length(max = Constants.MAX_USER_ID_LENGTH) final String userId) {
        return userStore.getById(userId)
                .map(userSummary -> redirect("/admin/users/" + userSummary.getId()))
                .orElseThrow(() -> fail("No user found for " + userId, USER_SEARCH_PATH));
    }

    @GET
    @Path("/users/{userId}")
    public Response renderUserAdminPage(
            @Auth ConductorUser user,
            @PathParam("userId") @NotEmpty final String userId) {
        return userLifecycleManager.userDetails(userId)
                .map(userDetails -> render(new UserAdminView(user.getUserSession().getUser(),
                                                             userDetails,
                                                             roleStore.list(),
                                                             groupStore.list().stream()
                                                                     .filter(group -> GroupType.MANUALLY_ASSIGNED.equals(
                                                                             group.getType()))
                                                                     .toList(),
                                                             skillStore.listSkillValues(),
                                                             attributeManager.read(AttributeScopeType.USER, userDetails.getSummary().getId()),
                                                             sessionStore.list(userId))))
                .orElseThrow(() -> fail("No user found for " + userId, USER_SEARCH_PATH));
    }

    @POST
    @Path("/users/{userId}/state")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response changeUserState(
            @Auth ConductorUser user,
            @PathParam("userId") @NotEmpty final String userId,
            @FormParam("state") @NotNull UserState state) {
        return userStore.updateState(userId, state)
                .map(userSummary -> redirect("/admin/users/" + userSummary.getId()))
                .orElseThrow(() -> fail("Could not update state for " + userId, USER_SEARCH_PATH));
    }

    @POST
    @Path("/users/{userId}/role")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response assignUserRole(
            @Auth ConductorUser user,
            @PathParam("userId") @NotEmpty final String userId,
            @FormParam("roleId") @NotEmpty String roleId) {
        if (!authConfig.isDisableRoleCheck() && user.getUserSession().getUser().getSummary().getId().equals(userId)) {
            throw fail("Cannot assign a role to yourself. Ask an administrator to do this for you. ", USER_SEARCH_PATH);
        }
        if (roleMappingStore.assignRoleToUser(userId, roleId)) {
            return redirect("/admin/users/" + userId);
        }
        throw fail("Could not update state for " + userId, USER_SEARCH_PATH);
    }

    @POST
    @Path("/users/{userId}/attributes")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response updateUserAttributes(
            @Auth ConductorUser user,
            @PathParam("userId") @NotEmpty final String userId,
            final MultivaluedMap<String, String> form) {
        val res = attributeManager.save(AttributeScopeType.USER, userId, form);
        val failures = res.getValidationResults()
                .values()
                .stream()
                .filter(vr -> vr.getStatus().equals(AttributeManager.AttributeValidationStatus.Status.FAILURE))
                .map(AttributeManager.AttributeValidationStatus.AttributeValidationResult::getMessage)
                .toList();
        if(failures.isEmpty()) {
            return redirect("/admin/users/" + userId);
        }
        throw fail("Failed to validate attributes: " + failures, "/admin/users/" + userId);
    }

    @POST
    @Path("/users/system")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response createSystemUser(
            @Auth ConductorUser user,
            @FormParam("sysUserName") @NotEmpty @Length(min = 1, max = 30) final String name,
            @FormParam("sysEmail") @Email @Length(min = 1, max = 30) final String email) {
        return userLifecycleManager.createSystemUser(name, email)
                .map(userDetails -> redirect("/admin/users/" + userDetails.getId()))
                .orElseThrow(() -> fail("Could not create user", USER_SEARCH_PATH));
    }

    @POST
    @Path("/users/system/{userId}/session/create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @RolesAllowed(Permission.Values.ADMIN)
    public Response createSystemUserSession( @Auth ConductorUser user,
                                             @PathParam("userId") @NotEmpty final String userId) {
        return userLifecycleManager.startSystemUserSession(userId)
                .map(session -> redirect("/admin/users/" + userId))
                .orElseThrow(() -> fail("Could not start session", "/admin/users/" + userId));
    }

    @GET
    @Path("/users/system/{userId}/session/{sessionId}/jwt")
    @RolesAllowed(Permission.Values.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response jwtForSession(@Auth ConductorUser user,
                                  @PathParam("userId") @NotEmpty final String userId,
                                  @PathParam("sessionId") @NotEmpty final String sessionId) {
        return userLifecycleManager.jwtForSession(userId, sessionId)
                .map(jwt -> Response.ok(Map.of("jwt", jwt)).build())
                .orElse(Response.noContent().build());
    }

    @POST
    @Path("/users/system/{userId}/session/{sessionId}/end")
    @RolesAllowed(Permission.Values.ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response endSystemUserSession(@Auth ConductorUser user,
                                  @PathParam("userId") @NotEmpty final String userId,
                                  @PathParam("sessionId") @NotEmpty final String sessionId) {
        val status = userLifecycleManager.completeUserSession(userId, sessionId);
        log.info("status for completion: {}/{} -> {}", userId, sessionId, status);
        return redirect("/admin/users/" + userId);
    }
}
