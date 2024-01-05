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

package io.appform.conductor.server.auth;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.UnauthorizedHandler;
import io.dropwizard.setup.Environment;
import lombok.val;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.net.URI;

@Provider
public class OAuthDynamicFeature extends AuthDynamicFeature {

    @Inject
    public OAuthDynamicFeature(ConductorAuthenticator authenticator,
                                ConductorAuthorizer authorizer,
                                Environment environment) {
        super(new ConductorAuthFilter.Builder()
                .setAuthenticator(authenticator)
                .setAuthorizer(authorizer)
                .setPrefix(ConductorAuthFilter.DEFAULT_PREFIX)
                      .setUnauthorizedHandler(new UnauthorizedHandler() {
                          @Nullable
                          @Override
                          public Response buildResponse(String prefix, String realm) {
                              val source = AuthSource.valueOf(realm);
                              return switch (source) {
                                  case UI -> Response.seeOther(URI.create("/user/login")).build();
                                  case API -> Response.status(Response.Status.UNAUTHORIZED).build();
                              };
                          }
                      })
                .buildAuthFilter());

        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(ConductorUser.class));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
    }
}
