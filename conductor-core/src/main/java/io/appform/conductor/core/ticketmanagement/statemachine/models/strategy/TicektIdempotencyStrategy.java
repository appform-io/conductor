package io.appform.conductor.core.ticketmanagement.statemachine.models.strategy;

public enum TicektIdempotencyStrategy {
    IGNORE,
    CHECK_FOR_SUBJECT,
    ;
}
