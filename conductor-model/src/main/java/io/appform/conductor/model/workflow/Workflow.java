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

import io.appform.conductor.model.schema.TicketState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Data
@AllArgsConstructor
public class Workflow {
    /**
     * Unique id for the workflow
     */
    private final String id;

    /**
     * Human-readable name of the workflow
     */
    private final String displayName;

    /**
     * Human-readable description of the workflow
     */
    private String description;

    /**
     * The schema for tickets in this workflow
     */
    private final String schemaId;

    /**
     * List of all states for this workflow
     */
    private final Map<String, TicketState> states;

    /**
     * State machine transitions in the workflow
     */
    private final Map<String, List<TicketStateTransition>> ticketStateTransitions;

    /**
     * Start state
     */
    private String startStateId;

    /**
     * Rules for selection of this workflow
     */
    private final Map<String, Rule> selectionRules;

    private WorkflowState state;

    private final Date created;
    private final Date updated;
}
