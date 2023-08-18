package io.appform.conductor.server.ticketmanagement;


enum TicketStrategy {

    GET_FROM_PROVIDED_ID(WorkflowStrategy.GET_FROM_TICKET, SubjectStrategy.GET_FROM_TICKET, SchemaStrategy.GET_FROM_WORKFLOW, SubjectIdempotencyStrategy.IGNORE_CHECK, AlreadyEndedStrategy.ABORT),
    GET_FROM_EXTRACTED_ID(null, null, null, null, null),
    CREATE_FROM_RAW_DATA(WorkflowStrategy.SELECT_WORKFLOW, SubjectStrategy.EXTRACT_FROM_RAW_DATA, SchemaStrategy.GET_FROM_WORKFLOW, SubjectIdempotencyStrategy.CHECK_FOR_SUBJECT, AlreadyEndedStrategy.CREATE_NEW),
    CREATE_FROM_GIVEN_DATA(WorkflowStrategy.GET_FROM_PROVIDED_ID, SubjectStrategy.GET_FROM_PROVIDED_ID, SchemaStrategy.GET_FROM_WORKFLOW, SubjectIdempotencyStrategy.CHECK_FOR_SUBJECT, AlreadyEndedStrategy.CREATE_NEW),
    ;
    WorkflowStrategy workflowStrategy;
    SubjectStrategy subjectStrategy;
    SchemaStrategy schemaStrategy;
    SubjectIdempotencyStrategy subjectIdempotencyStrategy;
    AlreadyEndedStrategy alreadyEndedStrategy;

    TicketStrategy(WorkflowStrategy workflowStrategy,
                   SubjectStrategy subjectStrategy,
                   SchemaStrategy schemaStrategy,
                   SubjectIdempotencyStrategy subjectIdempotencyStrategy,
                   AlreadyEndedStrategy alreadyEndedStrategy) {
        this.workflowStrategy = workflowStrategy;
        this.subjectStrategy = subjectStrategy;
        this.schemaStrategy = schemaStrategy;
        this.subjectIdempotencyStrategy = subjectIdempotencyStrategy;
        this.alreadyEndedStrategy = alreadyEndedStrategy;
    }

}

enum WorkflowStrategy {
    GET_FROM_PROVIDED_ID,
    SELECT_WORKFLOW,
    GET_FROM_TICKET,
    ;
}

enum SubjectStrategy {
    GET_FROM_PROVIDED_ID,
    EXTRACT_FROM_RAW_DATA,
    GET_FROM_TICKET,
    ;
}

enum SchemaStrategy {
    GET_FROM_WORKFLOW,
    ;
}

enum SubjectIdempotencyStrategy {
    IGNORE_CHECK,
    CHECK_FOR_SUBJECT,
    ;
}

enum AlreadyEndedStrategy {
    ABORT,
    CREATE_NEW,
    ;
}