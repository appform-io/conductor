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

package io.appform.conductor.server.ui.views.tasks;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.server.taskmanagement.model.Task;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RunActionOnCQLSelectView extends BaseLoggedInView {
    String workflowId;
    String query;
    List<Action> availableActions;
    Task task;

    public RunActionOnCQLSelectView(
            User currentUser,
            String workflowId,
            String query,
            List<Action> availableActions,
            Task task) {
        super("templates/tasks/run-action-on-cql.hbs", currentUser);
        this.workflowId = workflowId;
        this.query = query;
        this.availableActions = availableActions;
        this.task = task;
    }
}
