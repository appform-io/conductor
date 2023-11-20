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

package io.appform.conductor.server.eventmanagement;

import io.appform.conductor.server.eventmanagement.events.actions.ActionCreatedEvent;
import io.appform.conductor.server.eventmanagement.events.actions.ActionDeletedEvent;
import io.appform.conductor.server.eventmanagement.events.actions.ActionUpdatedEvent;
import io.appform.conductor.server.eventmanagement.events.group.GroupCreatedEvent;
import io.appform.conductor.server.eventmanagement.events.group.GroupDeletedEvent;
import io.appform.conductor.server.eventmanagement.events.group.GroupUpdatedEvent;
import io.appform.conductor.server.eventmanagement.events.reporting.ReportExecutionCompletedEvent;
import io.appform.conductor.server.eventmanagement.events.role.RoleCreatedEvent;
import io.appform.conductor.server.eventmanagement.events.role.RoleDeletedEvent;
import io.appform.conductor.server.eventmanagement.events.role.RoleUpdatedEvent;
import io.appform.conductor.server.eventmanagement.events.schema.*;
import io.appform.conductor.server.eventmanagement.events.skill.*;
import io.appform.conductor.server.eventmanagement.events.subject.*;
import io.appform.conductor.server.eventmanagement.events.ticket.*;
import io.appform.conductor.server.eventmanagement.events.user.*;
import io.appform.conductor.server.eventmanagement.events.workflow.*;

/**
 *
 */
public interface EventVisitor<T> {
    T visit(SkillCreatedEvent skillCreatedEvent);

    T visit(SkillDeletedEvent skillDeletedEvent);

    T visit(SkillUpdatedEvent skillUpdatedEvent);

    T visit(SkillValueAddedEvent skillValueAddedEvent);

    T visit(SkillValueRemovedEvent skillValueRemovedEvent);

    T visit(UserSkillAssociatedEvent userSkillAssociatedEvent);

    T visit(UserSkillDisasocciatedEvent userSkillDisasocciatedEvent);

    T visit(UserCreatedEvent userCreatedEvent);

    T visit(UserStateChangeEvent userStateChangeEvent);

    T visit(UserUpdatedEvent userUpdatedEvent);

    T visit(GroupCreatedEvent groupCreatedEvent);

    T visit(GroupDeletedEvent groupDeletedEvent);

    T visit(GroupUpdatedEvent groupUpdatedEvent);

    T visit(UserGroupAssignedEvent userGroupAssignedEvent);

    T visit(UserGroupUnassignedEvent userGroupUnassignedEvent);

    T visit(UserSessionUpdatedEvent userSessionUpdatedEvent);

    T visit(UserSessionCreatedEvent userSessionCreatedEvent);

    T visit(UserActivationTokenGeneratedEvent userActivationTokenGeneratedEvent);

    T visit(UserPasswordSetEvent userPasswordSetEvent);

    T visit(RoleCreatedEvent roleCreatedEvent);

    T visit(RoleUpdatedEvent roleUpdatedEvent);

    T visit(RoleDeletedEvent roleDeletedEvent);

    T visit(UserRoleAssignedEvent userRoleAssignedEvent);

    T visit(UserRoleRevokedEvent userRoleRevokedEvent);

    T visit(AddressCreatedEvent addressCreatedEvent);

    T visit(AddressDeletedEvent addressDeletedEvent);

    T visit(AddressUpdatedEvent addressUpdatedEvent);

    T visit(SubjectCreatedEvent subjectCreatedEvent);

    T visit(SubjectDeletedEvent subjectDeletedEvent);

    T visit(SubjectIdentifierCreatedEvent subjectIdentifierCreatedEvent);

    T visit(SubjectIdentifierDeletedEvent subjectIdentifierDeletedEvent);

    T visit(SubjectIdentifierMarkedPrimaryEvent subjectIdentifierMarkedPrimaryEvent);

    T visit(SubjectIdentifierUpdatedEvent subjectIdentifierUpdatedEvent);

    T visit(SubjectUpdatedEvent subjectUpdatedEvent);

    T visit(ActionDeletedEvent actionDeletedEvent);

    T visit(ActionCreatedEvent actionCreatedEvent);

    T visit(ActionUpdatedEvent actionUpdatedEvent);

    T visit(SchemaFieldAddedEvent schemaFieldAddedEvent);

    T visit(SchemaFieldDeletedEvent schemaFieldDeletedEvent);

    T visit(SchemaFieldUpdatedEvent schemaFieldUpdatedEvent);

    T visit(SchemaCreatedEvent schemaCreatedEvent);

    T visit(SchemaStateUpdatedEvent schemaStateUpdatedEvent);

    T visit(WorkflowCreatedEvent workflowCreatedEvent);

    T visit(WorkflowDeletedEvent workflowDeletedEvent);

    T visit(WorkflowSelectionRuleChangedEvent workflowSelectionRuleChangedEvent);

    T visit(WorkflowSelectionRuleDeletedEvent workflowSelectionRuleDeletedEvent);

    T visit(WorkflowStateChangedEvent workflowStateChangedEvent);

    T visit(WorkflowStateDeletedEvent workflowStateDeletedEvent);

    T visit(WorkflowTransitionDeletedEvent workflowTransitionDeletedEvent);

    T visit(WorkflowTransitionChangedEvent workflowTransitionChangedEvent);

    T visit(WorkflowUpdatedEvent workflowUpdatedEvent);

    T visit(AttachmentAddedEvent attachmentAddedEvent);

    T visit(AttachmentDeletedEvent attachmentDeletedEvent);

    T visit(CommentAddedEvent commentAddedEvent);

    T visit(TicketCreatedEvent ticketCreatedEvent);

    T visit(TicketUpdatedEvent ticketUpdatedEvent);

    T visit(TicketUserAssignedEvent ticketUserAssignedEvent);

    T visit(TicketGroupAssignedEvent ticketGroupAssignedEvent);

    T visit(TicketFieldsUpdatedEvent ticketFieldsUpdatedEvent);

    T visit(TicketPriorityUpdatedEvent ticketPriorityUpdatedEvent);

    T visit(TicketStateUpdatedEvent ticketStateUpdatedEvent);

    T visit(ReportExecutionCompletedEvent reportExecutionCompletedEvent);

    T visit(RelatedTicketAddedEvent relatedTicketAddedEvent);

    T visit(RelatedTicketDeletedEvent relatedTicketDeletedEvent);

    T visit(TicketExternalReferenceIDUpdated ticketExternalReferenceIDUpdated);
}
