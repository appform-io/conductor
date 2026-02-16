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

package io.appform.conductor.user.auth;

import io.appform.conductor.core.auth.ConductorUser;
import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.core.config.AuthConfig;
import io.dropwizard.auth.Authorizer;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * Authorizes a user based on permissions (s)he has
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConductorAuthorizer implements Authorizer<ConductorUser> {
    private final AuthConfig authConfig;

    @Override
    public boolean authorize(ConductorUser principal, String requiredPermission) {
        return authorize(principal, requiredPermission, null);
    }

    @Override
    public boolean authorize(
            ConductorUser principal,
            String requiredPermission,
            @Nullable ContainerRequestContext requestContext) {
        //We can do custom checks based on group membership, priority of permissions etc
        return authConfig.isDisableRoleCheck()
            || principal.getUserSession().getUser().getPermissions()
                .contains(Permission.valueOf(requiredPermission));
    }
}
