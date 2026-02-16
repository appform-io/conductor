package io.appform.conductor.core.ticketmanagement.statemachine;

import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.core.ticketmanagement.statemachine.models.TicketStateMachineContext;
import io.appform.conductor.core.ticketmanagement.statemachine.models.TriggerData;

public interface TransitionHandler {

    void beforeTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData triggerData);

    void onTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData triggerData);

    void afterTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData triggerData);

}
