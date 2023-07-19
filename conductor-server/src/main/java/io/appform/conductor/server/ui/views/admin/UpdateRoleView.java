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

package io.appform.conductor.server.ui.views.admin;

import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.Map;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UpdateRoleView extends BaseLoggedInView {
    String id;
    String name;
    String description;
    Map<Permission, Boolean> permissions;

    public UpdateRoleView(User user, String id, String name, String description, Map<Permission, Boolean> permissions) {
        super("templates/admin/role-update.hbs", user);
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = permissions;
    }
}
