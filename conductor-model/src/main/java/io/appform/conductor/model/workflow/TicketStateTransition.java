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

import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * A defined state transition in a workflow
 */
@Value
public class TicketStateTransition {

    /**
     * Behaviour of the state machine executor when it reaches this transition
     */
    public enum TicketStateTransitionType {

        /**
         * The associated rule will get evaluated to see if the transition is valid
         */
        EVALUATED,
        /**
         * This transition will be considered last and if none of the other {@link TicketStateTransitionType#EVALUATED}
         * transitions happened, this path will be taken.
         */
        DEFAULT
    }

    /**
     * Globally unique ID for the transition
     */
    String id;

    /**
     * Originating state of the transition
     */
    String from;

    /**
     * Terminating state of the transition
     */
    String to;

    /**
     * Type of this transition
     */
    TicketStateTransitionType type;

    /**
     * Actual rule to be evaluated to check if the transition can be made or not
     */
    Rule rule;

    /**
     * Actions to be performed when the transition is successful
     */
    List<String> actionIds;

    /**
     * Parent workflow ID
     */
    String workflowId;

    /**
     * Creation date for the transition
     */
    Date created;

    /**
     * Last updated date for this transition
     */
    Date updated;
}
