package io.appform.conductor.server.ticketmanagement.statemachine;

import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.server.ticketmanagement.TicketStateMachineContext;

public interface TransitionHandler {

    void beforeTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData triggerData);

    void onTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData triggerData);

    void afterTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData triggerData);

}
