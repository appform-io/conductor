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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 *
 */
@Data
@FieldNameConstants
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = TaskSpec.Fields.type)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "RUN_ACTION_ON_SELECTED_TICKETS", value = RunActionOnSelectedTicketsTaskSpec.class),
        @JsonSubTypes.Type(name = "RUN_ACTION_ON_CQL_SELECT", value = RunActionOnCQLSelectTaskSpec.class)
})
@RequiredArgsConstructor
public abstract class TaskSpec {
    private final TaskType type;

    public abstract  <T> T accept(final TaskSpecVisitor<T> task);

}
