package io.appform.conductor.server.ticketmanagement.statemachine;

import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.workflow.TicketStateTransition;

public interface TransitionHandler {

    TicketDetails beforeTransition(TicketStateTransition transition, TicketDetails ticket, Trigger trigger);

    TicketDetails onTransition(TicketStateTransition transition, TicketDetails ticket, Trigger trigger);

    TicketDetails afterTransition(TicketStateTransition transition, TicketDetails ticket, Trigger trigger);

}
