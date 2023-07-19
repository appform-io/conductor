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

import io.appform.conductor.model.usermgmt.UserSession;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.server.auth.ConductorAuthFilter;
import io.appform.conductor.server.ui.views.ActivationView;
import io.appform.conductor.server.usermanagement.UserLifecycleManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.vyarus.guicey.gsp.views.template.Template;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.inject.Inject;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 *
 */
@Slf4j
@Path("/ui/login")
@Template
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class Login {

    private final UserLifecycleManager userLifecycleManager;

    @GET
    public TemplateView loginPage() {
        return new TemplateView("templates/login.hbs");
    }

    @Path("/register")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response registerAccount(
            @FormParam("newName") @NotEmpty final String newName,
            @FormParam("newEmail") @NotEmpty @Email final String newEmail,
            @FormParam("newPassword") @NotEmpty final String newPassword) {
        return userLifecycleManager.createHumanUser(newName, newEmail, newPassword)
                .map(UserSummary::getId)
                .flatMap(userLifecycleManager::openToken)
                .map(token -> Response.seeOther(URI.create("/login/activate/" + token.getToken())).build())
                .orElse(Response.seeOther(URI.create("/")).build());
    }

    @Path("/activate/{token}")
    @GET
    public Response renderActivationScreen(@PathParam("token") final String token) {
        return userLifecycleManager.showToken(token)
                .map(userSummary -> Response.ok(new ActivationView(token, userSummary.getName())).build())
                .orElse(Response.seeOther(URI.create("/")).build());
    }


    @Path("/activate/{token}")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response activateToken(@PathParam("token") final String token,
                                  @FormParam("password") final String password) {
        return userLifecycleManager.activateUser(token, password)
                .map(Login::newSessionResponse)
                .orElse(Response.seeOther(URI.create("/")).build());
    }

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @POST
    public Response login(@FormParam("email") final String email,
                          @FormParam("password") final String password) {
        return userLifecycleManager.loginUser(email, password)
                .map(Login::newSessionResponse)
                .orElse(Response.seeOther(URI.create("/")).build());
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
}
