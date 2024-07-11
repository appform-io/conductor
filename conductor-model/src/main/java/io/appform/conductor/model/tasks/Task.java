/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.model.tasks;

import io.appform.conductor.model.actions.Scope;
import lombok.*;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

/**
 *
 */
@Value
@With
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Task {
    String id;
    TaskType type;
    String name;
    String description;
    String cron;
    Scope scope;
    TaskState state;
    TaskSpec spec;
    Date lastExecutionCompletionTime;
    TaskRunStatus lastRunStatus;
    Map<String, Object> taskMeta;
    Date created;
    Date updated;
}
