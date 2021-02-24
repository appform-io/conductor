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

package io.appform.conductor.model.ticket;

import io.appform.conductor.model.schema.State;
import io.appform.conductor.model.subject.Subject;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.workflow.Workflow;
import lombok.Value;

import java.util.Date;

/**
 * A summary for a ticket
 */
@Value
public class TicketSummary {

    /**
     * Globally unique id for the ticket
     */
    String id;

    /**
     * A summary title for the ticket
     */
    String title;

    /**
     * Ticket details
     */
    String description;

    /**
     * The {@link Workflow} associated for this ticket
     */
    String workflowId;

    /**
     * User that created this ticket
     */
    UserSummary createdBy;

    /**
     * Currently assigned {@link io.appform.conductor.model.usermgmt.Group}
     */
    Group assignedToGroup;

    /**
     * Currently assigned {@link io.appform.conductor.model.usermgmt.User}
     */
    UserSummary assignedToUser;

    /**
     * The subject whom this ticket is about
     */
    Subject subject;

    /**
     * Current ticket {@link State}
     */
    State state;

    /**
     * Priority for the ticket
     */
    TicketPriority priority;

    /**
     * Date when ticket was created
     */
    Date created;

    /**
     * Date when ticket was last updated
     */
    Date updated;
}
