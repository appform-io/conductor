/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.model.workflow;

import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.State;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 *
 */
@Value
public class Workflow {
    /**
     * Unique id for the workflow
     */
    String id;

    /**
     * Human-readable name of the workflow
     */
    String name;

    /**
     * Human-readable description of the workflow
     */
    String description;

    /**
     * The schema for tickets in this workflow
     */
    Schema schema;

    /**
     * state machine transitions in the workflow
     */
    List<StateTransition> stateTransitions;

    /**
     * Start state
     */
    State start;
    Date created;
    Date updated;
}
