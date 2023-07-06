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

package io.appform.conductor.server.ticketmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.TicketSummary;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.usermanagement.GroupStore;
import io.appform.conductor.server.usermanagement.UserStore;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.workflowmanagement.WorkflowSelector;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class TicketManager {
    private final TicketStore ticketStore;
    private final SchemaStore schemaStore;
    private final WorkflowStore workflowStore;
    private final UserStore userStore;
    private final GroupStore groupStore;
    private final SubjectStore subjectStore;
    private final ActionStore actionStore;
    private final WorkflowSelector workflowSelector;
    private final TicketFieldMapper fieldMapper;
    private final ObjectMapper mapper;

    @SneakyThrows
    public Optional<TicketDetails> createTicket(final Object payload) {
        val data = mapper.valueToTree(payload);
        val workflow = workflowSelector.findWorkflow(data).orElse(null);
        ConductorServerUtils.ensureCondition(workflow != null, ConductorErrorCode.TICKET_MGMT_NO_WORKFLOW);
        val schema = schemaStore.get(workflow.getSchemaId()).orElse(null);
        ConductorServerUtils.ensureCondition(schema != null,
                                             ConductorErrorCode.TICKET_MGMT_NO_SCHEMA,
                                             Map.of("workflowId", workflow.getId(),
                                                    "schemaId", schema.getId()));
        val fieldMappingResult = fieldMapper.map(schema, data);
        ConductorServerUtils.ensureCondition(fieldMappingResult.getErrors().isEmpty(),
                                             ConductorErrorCode.TICKET_SCHEMA_VALIDATION_FAILURE,
                                             Map.of("errors", fieldMappingResult.getErrors()));
        return ticketStore.create(UUID.randomUUID().toString(),
                                                null,
                                                null,
                                                workflow.getId(),
                                                null,
                                                workflow.getStartStateId(),
                                                TicketPriority.MEDIUM,
                                                Objects.requireNonNullElse(fieldMappingResult.getData(), List.of()))
                .map(skeleton -> ticketDetails(skeleton, workflow));
    }

    private TicketDetails ticketDetails(final TicketSkeleton skeleton, final Workflow workflow) {
        return new TicketDetails(new TicketSummary(skeleton.getTicketId(),
                                                   skeleton.getTitle(),
                                                   skeleton.getDescription(),
                                                   skeleton.getWorkflowId(),
                                                   userStore.getById(skeleton.getTicketId()).orElse(null),
                                                   Objects.nonNull(skeleton.getAssignedToGroupId())
                                                    ? groupStore.get(skeleton.getAssignedToGroupId()).orElse(null)
                                                   : null,
                                                   Objects.nonNull(skeleton.getAssignedToUserId())
                                                    ? userStore.getById(skeleton.getAssignedToUserId()).orElse(null)
                                                   : null,
                                                   Objects.nonNull(skeleton.getSubjectId())
                                                    ? subjectStore.getSubject(skeleton.getSubjectId()).orElse(null)
                                                   : null,
                                                   workflow.getStates().get(skeleton.getTicketStateId()),
                                                   skeleton.getPriority(),
                                                   skeleton.getCreated(),
                                                   skeleton.getUpdated()),
                                 skeleton.getFields(),
                                 List.of(), //TODO::COMMENTS
                                 List.of()); //TODO::ACTION
    }

}
