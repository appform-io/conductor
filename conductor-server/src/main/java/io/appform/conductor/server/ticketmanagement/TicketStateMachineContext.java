package io.appform.conductor.server.ticketmanagement;

import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.subject.SubjectSummary;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.workflow.Workflow;
import lombok.Data;

@Data
public class TicketStateMachineContext {

    private Workflow workflow;
    private Schema schema;
    private TicketDetails ticketDetails;
    private TicketSkeleton ticketSkeleton;
    private SubjectSummary subject;

}
