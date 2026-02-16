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

package io.appform.conductor.core.ticketmanagement;

import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.ExternalReferenceID;
import io.appform.conductor.model.ticket.fields.TicketField;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Date;
import java.util.List;

/**
 * A skeleton object to be used to store ticket information in storage.
 * Rest of the data will be augmented by upper layers
 */
@Data
@FieldNameConstants
public class TicketSkeleton {
    private String ticketId;

    private String title;

    private String description;

    private String workflowId;

    private String createdByUserId;

    private String assignedToGroupId;

    private String assignedToUserId;

    private String subjectId;

    private String ticketStateId;

    private TicketPriority priority;

    private List<TicketField> fields;

    private ExternalReferenceID externalReferenceID;

    private List<String> ticketActionsIds;

    private boolean deleted;

    private Date created;

    private Date updated;
}
