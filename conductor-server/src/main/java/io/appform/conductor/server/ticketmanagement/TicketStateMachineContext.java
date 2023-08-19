package io.appform.conductor.server.ticketmanagement;

import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.subject.SubjectSummary;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.workflow.Workflow;
import io.appform.conductor.server.schemamanagement.SchemaOpValidationResult;
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
    private UserSummary ticketAssignedToUser;

    public String ticketId() {
        return this.ticketSkeleton.getTicketId();
    }

    public TicketState currentState() {
        return this.workflow.getStates().get(this.ticketSkeleton.getTicketStateId());
    }

}
