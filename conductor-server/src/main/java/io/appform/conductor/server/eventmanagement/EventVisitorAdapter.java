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

import io.appform.conductor.server.eventmanagement.events.*;
import io.appform.conductor.server.eventmanagement.events.reporting.ReportExecutionCompletedEvent;

/**
 *
 */
public abstract class EventVisitorAdapter<T> implements EventVisitor<T> {
    private final T defaultValue;

    protected EventVisitorAdapter() {
        this(null);
    }
    
    protected EventVisitorAdapter(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public T visit(SkillCreatedEvent skillCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SkillDeletedEvent skillDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SkillUpdatedEvent skillUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SkillValueAddedEvent skillValueAddedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SkillValueRemovedEvent skillValueRemovedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserSkillAssociatedEvent userSkillAssociatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserSkillDisasocciatedEvent userSkillDisasocciatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserCreatedEvent userCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserStateChangeEvent userStateChangeEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserUpdatedEvent userUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(GroupCreatedEvent groupCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(GroupDeletedEvent groupDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(GroupUpdatedEvent groupUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserGroupAssignedEvent userGroupAssignedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserGroupUnassignedEvent userGroupUnassignedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserSessionUpdatedEvent userSessionUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserSessionCreatedEvent userSessionCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserActivationTokenGeneratedEvent userActivationTokenGeneratedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserPasswordSetEvent userPasswordSetEvent) {
        return defaultValue;
    }

    @Override
    public T visit(RoleCreatedEvent roleCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(RoleUpdatedEvent roleUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(RoleDeletedEvent roleDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserRoleAssignedEvent userRoleAssignedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(UserRoleRevokedEvent userRoleRevokedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(AddressCreatedEvent addressCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(AddressDeletedEvent addressDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(AddressUpdatedEvent addressUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SubjectCreatedEvent subjectCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SubjectDeletedEvent subjectDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SubjectIdentifierCreatedEvent subjectIdentifierCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SubjectIdentifierDeletedEvent subjectIdentifierDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SubjectIdentifierMarkedPrimaryEvent subjectIdentifierMarkedPrimaryEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SubjectIdentifierUpdatedEvent subjectIdentifierUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SubjectUpdatedEvent subjectUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(ActionDeletedEvent actionDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(ActionCreatedEvent actionCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(ActionUpdatedEvent actionUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SchemaFieldAddedEvent schemaFieldAddedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SchemaFieldDeletedEvent schemaFieldDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SchemaFieldUpdatedEvent schemaFieldUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SchemaCreatedEvent schemaCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(SchemaStateUpdatedEvent schemaStateUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowCreatedEvent workflowCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowDeletedEvent workflowDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowSelectionRuleChangedEvent workflowSelectionRuleChangedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowSelectionRuleDeletedEvent workflowSelectionRuleDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowStateChangedEvent workflowStateChangedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowStateDeletedEvent workflowStateDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowTransitionDeletedEvent workflowTransitionDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowTransitionChangedEvent workflowTransitionChangedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(WorkflowUpdatedEvent workflowUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(AttachmentAddedEvent attachmentAddedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(AttachmentDeletedEvent attachmentDeletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(CommentAddedEvent commentAddedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(TicketCreatedEvent ticketCreatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(TicketUpdatedEvent ticketUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(TicketUserAssignedEvent ticketUserAssignedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(TicketGroupAssignedEvent ticketGroupAssignedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(TicketFieldsUpdatedEvent ticketFieldsUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(TicketPriorityUpdatedEvent ticketPriorityUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(TicketStateUpdatedEvent ticketStateUpdatedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(ReportExecutionCompletedEvent reportExecutionCompletedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(RelatedTicketAddedEvent relatedTicketAddedEvent) {
        return defaultValue;
    }

    @Override
    public T visit(RelatedTicketDeletedEvent relatedTicketDeletedEvent) {
        return defaultValue;
    }
}
