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

package io.appform.conductor.server.taskmanagement.model;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RunActionOnCQLSelectTaskSpec extends TaskSpec {
    String query;
    List<String> actionIds;

    @Builder
    @Jacksonized
    public RunActionOnCQLSelectTaskSpec(String query, List<String> actionIds) {
        super(TaskType.RUN_ACTION_ON_CQL_SELECT);
        this.query = query;
        this.actionIds = actionIds;
    }

    @Override
    public <T> T accept(TaskSpecVisitor<T> task) {
        return task.visit(this);
    }
}
