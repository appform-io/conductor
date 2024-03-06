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

import io.appform.conductor.model.usermgmt.UserSession;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.server.auth.ConductorAuthFilter;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.ui.views.ActivationView;
import io.appform.conductor.server.ui.views.user.UserAccountView;
import io.appform.conductor.server.usermanagement.UserLifecycleManager;
import io.appform.conductor.server.utils.Constants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.validator.constraints.Length;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.appform.conductor.server.utils.ConductorServerUtils.*;

/**
 *
 */
@Slf4j
@Path("/ui/user")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UIUserLifecycle {

    private final UserLifecycleManager userLifecycleManager;

    @GET
    @Path("/login")
    public TemplateView loginPage() {
        return new TemplateView("templates/login.hbs");
    }

    @Path("/register")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response registerAccount(
            @FormParam("newName") @NotEmpty @Length(max = 127) final String newName,
            @FormParam("newEmail") @NotEmpty @Length(max = 127) @Email final String newEmail,
            @FormParam("newPassword") @NotEmpty final String newPassword) {
        return userLifecycleManager.createHumanUser(newName, newEmail, newPassword)
                .map(UserSummary::getId)
                .flatMap(userLifecycleManager::openToken)
                .map(token -> redirect("/user/activate"))
                .orElseThrow(() -> fail("User registration failed for " + newName, "/"));
    }

    @Path("/activate")
    @GET
    public Response renderActivationScreen() {
        return render(new ActivationView());
    }


    @Path("/activate")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response activateToken(
            @FormParam("token") final String token,
            @FormParam("password") final String password) {
        return userLifecycleManager.activateUser(token, password)
                .map(UIUserLifecycle::newSessionResponse)
                .orElseThrow(() -> fail("Could not activate token: " + token, "/user/activate"));
    }

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/login")
    @POST
    public Response login(
            @FormParam("email") @Email @NotEmpty @Length(max = Constants.MAX_EMAIL_ID_LENGTH) final String email,
            @FormParam("password") @NotEmpty @Length(max = Constants.MAX_PASSWORD_LENGTH) final String password) {
        return userLifecycleManager.loginUser(email, password)
                .map(UIUserLifecycle::newSessionResponse)
                .orElseThrow(() -> fail("Login Failure", "/"));
    }


    @GET
    @Path("/logout")
    @PermitAll
    public Response logout(@Auth final ConductorUser user) {
        if (userLifecycleManager.logoutUser(user.getUserSession())) {
            return logout();
        }
        return Response.seeOther(URI.create("/")).build();
    }

    @GET
    public Response userDetails(@Auth final ConductorUser user) {
        val userId = user.getUserSession().getUser().getSummary().getId();
        return userLifecycleManager.userDetails(userId)
                .map(userDetails -> render(new UserAccountView(userDetails,
                                                               userDetails)))
                .orElseThrow(() -> fail("Unkown user " + userId, "/"));

    }

    @POST
    @Path("/name")
    public Response updateName(
            @Auth final ConductorUser user,
            @FormParam("name") @NotEmpty @Length(max = 255) final String name) {
        val userId = user.getUserSession().getUser().getSummary().getId();
        return userLifecycleManager.updateUserName(userId, name)
                .map(updated -> redirect("/user"))
                .orElseThrow(() -> fail("Could not update name for " + userId, "/user"));
    }

    @POST
    @Path("/password")
    public Response updatePassword(
            @Auth final ConductorUser user,
            @FormParam("oldPassword") @NotEmpty @Length(max = 255) final String oldPassword,
            @FormParam("newPassword") @NotEmpty @Length(max = 255) final String newPassword) {
        val userId = user.getUserSession().getUser().getSummary().getId();
        if (userLifecycleManager.changePassword(userId, oldPassword, newPassword)) {
            return redirect("/user");
        }
        throw fail("Could not update name for " + userId, "/user");
    }

    private static Response newSessionResponse(UserSession userSession) {
        return Response.seeOther(URI.create("/"))
                .cookie(new NewCookie(ConductorAuthFilter.COOKIE_NAME,
                                      userSession.getJwt(),
                                      "/",
                                      null,
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      NewCookie.DEFAULT_MAX_AGE,
                                      null,
                                      false,
                                      true))
                .build();
    }

    private static Response logout() {
        return Response.seeOther(URI.create("/"))
                .cookie(new NewCookie(ConductorAuthFilter.COOKIE_NAME,
                                      "",
                                      "/",
                                      null,
                                      Cookie.DEFAULT_VERSION,
                                      null,
                                      NewCookie.DEFAULT_MAX_AGE,
                                      null,
                                      false,
                                      true))
                .build();
    }
}
