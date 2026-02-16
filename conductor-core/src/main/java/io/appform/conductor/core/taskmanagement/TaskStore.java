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

package io.appform.conductor.core.taskmanagement;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.tasks.Task;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 *
 */
public interface TaskStore {
    Optional<Task> createOrUpdate(String id, final Task task);
    Optional<Task> update(final String id, final UnaryOperator<Task> updater);
    boolean delete(final String id);

    Optional<Task> read(final String taskId);

    List<Task> listByIds(List<String> ids);
    List<Task> listByScopes(List<Scope> scopes);
}
