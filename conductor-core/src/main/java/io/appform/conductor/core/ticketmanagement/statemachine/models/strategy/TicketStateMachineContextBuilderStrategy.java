package io.appform.conductor.server.ticketmanagement.statemachine.models.strategy;


import lombok.Getter;

@Getter
public enum TicketStateMachineContextBuilderStrategy {

    RAW_DATA(TicketFetchStrategy.NEW,
            WorkflowFetchStrategy.FROM_RULE,
            SubjectFetchStrategy.FROM_RAW_DATA,
            SchemaFetchStrategy.FROM_WORKFLOW,
            TicektIdempotencyStrategy.CHECK_FOR_SUBJECT,
            TicketTerminalStateStrategy.CREATE_NEW,
            TicketMetaDataFetchStrategy.FROM_TEMPLATE)
    ,
    CALLBACK(TicketFetchStrategy.FROM_PROVIDED_ID,
            WorkflowFetchStrategy.FROM_TICKET,
            SubjectFetchStrategy.FROM_TICKET,
            SchemaFetchStrategy.FROM_WORKFLOW,
            TicektIdempotencyStrategy.IGNORE,
            TicketTerminalStateStrategy.ABORT,
            TicketMetaDataFetchStrategy.FROM_TEMPLATE)
    ,
    FILE(TicketFetchStrategy.FROM_RULE,
            WorkflowFetchStrategy.FROM_TICKET,
            SubjectFetchStrategy.FROM_TICKET,
            SchemaFetchStrategy.FROM_WORKFLOW,
            TicektIdempotencyStrategy.IGNORE,
            TicketTerminalStateStrategy.CREATE_NEW,
            TicketMetaDataFetchStrategy.FROM_TEMPLATE)
    ,
    CONSOLE(TicketFetchStrategy.NEW,
            WorkflowFetchStrategy.FROM_PROVIDED_ID,
            SubjectFetchStrategy.FROM_PROVIDED_DATA,
            SchemaFetchStrategy.FROM_WORKFLOW,
            TicektIdempotencyStrategy.IGNORE,
            TicketTerminalStateStrategy.CREATE_NEW,
            TicketMetaDataFetchStrategy.FROM_PROVIDED_DATA)
    ,
    CONSOLE_UPDATE(TicketFetchStrategy.FROM_PROVIDED_ID,
            WorkflowFetchStrategy.FROM_TICKET,
            SubjectFetchStrategy.FROM_TICKET,
            SchemaFetchStrategy.FROM_WORKFLOW,
            TicektIdempotencyStrategy.IGNORE,
            TicketTerminalStateStrategy.ABORT,
            TicketMetaDataFetchStrategy.FROM_TICKET)
    ;
    TicketFetchStrategy ticketFetchStrategy;
    WorkflowFetchStrategy workflowFetchStrategy;
    SubjectFetchStrategy subjectFetchStrategy;
    SchemaFetchStrategy schemaFetchStrategy;
    TicektIdempotencyStrategy ticektIdempotencyStrategy;
    TicketTerminalStateStrategy ticketTerminalStateStrategy;
    TicketMetaDataFetchStrategy ticketMetaDataFetchStrategy;


    TicketStateMachineContextBuilderStrategy(TicketFetchStrategy ticketFetchStrategy,
                                             WorkflowFetchStrategy workflowFetchStrategy,
                                             SubjectFetchStrategy subjectFetchStrategy,
                                             SchemaFetchStrategy schemaFetchStrategy,
                                             TicektIdempotencyStrategy ticektIdempotencyStrategy,
                                             TicketTerminalStateStrategy ticketTerminalStateStrategy,
                                             TicketMetaDataFetchStrategy ticketMetaDataFetchStrategy) {
        this.ticketFetchStrategy = ticketFetchStrategy;
        this.workflowFetchStrategy = workflowFetchStrategy;
        this.subjectFetchStrategy = subjectFetchStrategy;
        this.schemaFetchStrategy = schemaFetchStrategy;
        this.ticektIdempotencyStrategy = ticektIdempotencyStrategy;
        this.ticketTerminalStateStrategy = ticketTerminalStateStrategy;
        this.ticketMetaDataFetchStrategy = ticketMetaDataFetchStrategy;
    }

}


