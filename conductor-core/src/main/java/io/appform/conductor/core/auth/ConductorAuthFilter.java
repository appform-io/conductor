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

import com.google.common.base.Strings;
import io.dropwizard.auth.AuthFilter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Optional;

/**
 *
 */
@Slf4j
@Priority(Priorities.AUTHENTICATION)
public class ConductorAuthFilter extends AuthFilter<String, ConductorUser> {
    public static final String COOKIE_NAME = "conductor-auth";
    public static final String DEFAULT_PREFIX = "Bearer";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        val source = requestContext.getUriInfo().getPath().startsWith("ui/") ? AuthSource.UI : AuthSource.API;
        val token = credentialsFromHeader(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
                .or(() -> credentialsFromCookie(requestContext.getCookies().get(COOKIE_NAME)))
                .orElseThrow(() -> unauthorizedHandler.buildException(prefix, source.name()));
        if(authenticate(requestContext, token.getToken(), token.getSource().name())) {
            return;
        }
        throw unauthorizedHandler.buildException(prefix, token.getSource().name());

    }

    private Optional<AuthData> credentialsFromHeader(String header) {
        if (Strings.isNullOrEmpty(header)) {
            return Optional.empty();
        }

        val parts = header.split("[ \\t]");

        if (parts.length != 2) {
            return Optional.empty();
        }
        if (!prefix.equalsIgnoreCase(parts[0])) {
            log.error("Authentication header does not have required prefix {}. Header value: {}",
                      prefix, header);
            return Optional.empty();
        }

        return Optional.of(new AuthData(AuthSource.API, parts[1]));
    }

    private Optional<AuthData> credentialsFromCookie(Cookie cookie) {
        if (null == cookie || Strings.isNullOrEmpty(cookie.getValue())) {
            return Optional.empty();
        }

        return Optional.of(new AuthData(AuthSource.UI, cookie.getValue()));
    }

    public static final class Builder extends AuthFilterBuilder<String, ConductorUser, ConductorAuthFilter> {

        @Override
        protected ConductorAuthFilter newInstance() {
            return new ConductorAuthFilter();
        }
    }
}
