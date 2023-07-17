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

import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.model.auth.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * A store for {@link Role}
 */
public interface RoleStore {
    Optional<Role> create(
            String roleId, final String name,
            final String description,
            final Set<Permission> permissions);

    Optional<Role> read(String roleId);

    List<Role> list();

    Set<Permission> permissionsForRoles(Collection<String> roleIds);

    Optional<Role> update(String roleId, UnaryOperator<Role> handler);

    boolean delete(String roleId);
}
