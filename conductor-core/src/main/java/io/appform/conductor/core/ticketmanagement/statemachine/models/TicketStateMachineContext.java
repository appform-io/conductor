package io.appform.conductor.core.ticketmanagement.statemachine.models;

import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.subject.SubjectSummary;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.core.schemamanagement.SchemaOpValidationResult;
import io.appform.conductor.core.ticketmanagement.TicketFieldData;
import io.appform.conductor.core.ticketmanagement.TicketSkeleton;
import lombok.Data;

import java.util.List;

@Data
public class TicketStateMachineContext {

    private Workflow workflow;
    private Schema schema;
    private TicketSkeleton ticketSkeleton;
    private SubjectSummary subject;
    private SchemaOpValidationResult<List<TicketFieldData>> fieldMappingResult;
    private UserSummary ticketCreatedBy;
    private Group ticketAssignedToGroup;
    private User ticketAssignedToUser;

    public String ticketId() {
        return this.ticketSkeleton.getTicketId();
    }

    public TicketState currentState() {
        return this.workflow.getStates().get(this.ticketSkeleton.getTicketStateId());
    }

}
