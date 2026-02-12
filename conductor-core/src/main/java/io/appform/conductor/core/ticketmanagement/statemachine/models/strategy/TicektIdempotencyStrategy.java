package io.appform.conductor.server.ticketmanagement.statemachine.models.strategy;

public enum TicektIdempotencyStrategy {
    IGNORE,
    CHECK_FOR_SUBJECT,
    ;
}
