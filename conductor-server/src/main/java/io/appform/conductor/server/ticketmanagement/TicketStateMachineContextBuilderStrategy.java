package io.appform.conductor.server.ticketmanagement;


enum TicketStateMachineContextBuilderStrategy {

    RAW_DATA(TicketStrategy.NEW,
            WorkflowStrategy.FROM_RULE,
            SubjectStrategy.FROM_RAW_DATA,
            SchemaStrategy.FROM_WORKFLOW,
            IdempotencyStrategy.CHECK_FOR_SUBJECT,
            TerminalTicketStrategy.CREATE_NEW,
            TicketMetaDataStrategy.FROM_TEMPLATE)
    ,
    CALLBACK(TicketStrategy.FROM_PROVIDED_ID,
            WorkflowStrategy.FROM_TICKET,
            SubjectStrategy.FROM_TICKET,
            SchemaStrategy.FROM_WORKFLOW,
            IdempotencyStrategy.IGNORE,
            TerminalTicketStrategy.ABORT,
            TicketMetaDataStrategy.FROM_TEMPLATE)
    ,
    FILE(TicketStrategy.FROM_RULE,
            WorkflowStrategy.FROM_TICKET,
            SubjectStrategy.FROM_TICKET,
            SchemaStrategy.FROM_WORKFLOW,
            IdempotencyStrategy.IGNORE,
            TerminalTicketStrategy.CREATE_NEW,
            TicketMetaDataStrategy.FROM_TEMPLATE)
    ,
    CONSOLE(TicketStrategy.NEW,
            WorkflowStrategy.FROM_PROVIDED_ID,
            SubjectStrategy.FROM_PROVIDED_DATA,
            SchemaStrategy.FROM_WORKFLOW,
            IdempotencyStrategy.IGNORE,
            TerminalTicketStrategy.CREATE_NEW,
            TicketMetaDataStrategy.FROM_PROVIDED_DATA)
    ,
    CONSOLE_UPDATE(TicketStrategy.FROM_PROVIDED_ID,
            WorkflowStrategy.FROM_TICKET,
            SubjectStrategy.FROM_TICKET,
            SchemaStrategy.FROM_WORKFLOW,
            IdempotencyStrategy.IGNORE,
            TerminalTicketStrategy.ABORT,
            TicketMetaDataStrategy.FROM_TICKET)
    ;
    TicketStrategy ticketStrategy;
    WorkflowStrategy workflowStrategy;
    SubjectStrategy subjectStrategy;
    SchemaStrategy schemaStrategy;
    IdempotencyStrategy idempotencyStrategy;
    TerminalTicketStrategy terminalTicketStrategy;
    TicketMetaDataStrategy ticketMetaDataStrategy;


    TicketStateMachineContextBuilderStrategy(TicketStrategy ticketStrategy,
                                             WorkflowStrategy workflowStrategy,
                                             SubjectStrategy subjectStrategy,
                                             SchemaStrategy schemaStrategy,
                                             IdempotencyStrategy idempotencyStrategy,
                                             TerminalTicketStrategy terminalTicketStrategy,
                                             TicketMetaDataStrategy ticketMetaDataStrategy) {
        this.ticketStrategy = ticketStrategy;
        this.workflowStrategy = workflowStrategy;
        this.subjectStrategy = subjectStrategy;
        this.schemaStrategy = schemaStrategy;
        this.idempotencyStrategy = idempotencyStrategy;
        this.terminalTicketStrategy = terminalTicketStrategy;
        this.ticketMetaDataStrategy = ticketMetaDataStrategy;
    }

}


enum TicketStrategy {
    FROM_PROVIDED_ID,
    FROM_RULE,
    NEW,
    ;
}

enum WorkflowStrategy {
    FROM_PROVIDED_ID,
    FROM_RULE,
    FROM_TICKET,
    ;
}

enum SubjectStrategy {
    FROM_PROVIDED_DATA,
    FROM_RAW_DATA,
    FROM_TICKET,
    ;
}

enum TicketMetaDataStrategy {
    FROM_TEMPLATE,
    FROM_PROVIDED_DATA,
    FROM_TICKET,
    ;
}

enum SchemaStrategy {
    FROM_WORKFLOW,
    ;
}

enum IdempotencyStrategy {
    IGNORE,
    CHECK_FOR_SUBJECT,
    ;
}

enum TerminalTicketStrategy {
    ABORT,
    CREATE_NEW,
    ;
}