package io.appform.conductor.server.ticketmanagement.statemachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.server.ruleengines.RuleEngine;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TicketStateMachine {

    private final ObjectMapper mapper;
    private final RuleEngine ruleEngine; //TODO:fix this way to remove this from sm and use it from outside
    private final Map<String, List<TicketStateTransition>> transitions;
    private final Schema schema;
    private final TransitionHandler executor;
    @Getter
    private TicketDetails ticket;
    private TicketState currentState;
    private Set<String> transitionAuditLog;

    public TicketStateMachine(
            Map<String, List<TicketStateTransition>> transitions,
            Schema schema,
            TicketDetails ticket,
            ObjectMapper mapper,
            RuleEngine ruleEngine,
            TransitionHandler executor) {
        this.mapper = mapper;
        this.ruleEngine = ruleEngine;
        this.executor = executor;
        this.transitions = transitions;
        this.schema = schema;
        this.ticket = ticket;
        this.transitionAuditLog = new HashSet<>();
        this.currentState = ticket.getSummary().getTicketState();
    }

    public void trigger(TicketEvent event, TriggerStrategy strategy) {
        if (strategy == TriggerStrategy.EXECUTE_FIRST) {
            trigger(event);
        } else if (strategy == TriggerStrategy.EXECUTE_ALL) {
            while (trigger(event)) {
                log.info("Triggering next transition for ticket: {}", this.ticket.getSummary().getId());
            }
        }
    }

    private boolean trigger(TicketEvent event) {
        if(this.currentState.isTerminal()) {
            log.info("Ticket {} is already in terminal state {}",
                    this.ticket.getSummary().getId(),
                    this.currentState.getDisplayName());
            return true;
        }
        val transition = selectTransition(event);
        if (transition == null) {
            log.info("No possible transitions found for ticket: {}. Will stop state machine execution.",
                    this.ticket.getSummary().getId());
            return false;
        }
        log.debug("Found transition: {}", transition);
        beforeTransition(transition, this.ticket, event);
        onTransition(transition, this.ticket, event);
        afterTransition(transition, this.ticket, event);
        return false;
    }

    private void beforeTransition(TicketStateTransition transition, TicketDetails ticket, TicketEvent event) {
       this.ticket = executor.beforeTransition(transition, ticket, event);
    }


    private void onTransition(TicketStateTransition transition, TicketDetails ticket, TicketEvent event) {
        this.ticket = executor.onTransition(transition, ticket, event);
        this.currentState = ticket.getSummary().getTicketState();
    }


    private void afterTransition(TicketStateTransition transition, TicketDetails ticket, TicketEvent event) {
        this.ticket = executor.afterTransition(transition, ticket, event);
        this.transitionAuditLog.add(transition.getId());
    }

    private TicketStateTransition selectTransition(TicketEvent event) {
        //TODO: fix move this out along with mapper and schema
        val evalDataJson = mapper.createObjectNode();
        evalDataJson.set("ticket", ConductorServerUtils.ticketToJsonNode(mapper, this.ticket, this.schema));
        evalDataJson.set("payload", event.getPayload());

        log.debug("Evaluating transitions for ticket: {} with data: {}",
                this.ticket.getSummary().getId(),
                evalDataJson);

        //filter out transitions already transitioned to avoid loops
        List<TicketStateTransition> eligibleTransitions = transitions.get(currentState.getId())
                .stream()
                .filter(transition -> transitionAuditLog.contains(transition.getId()))
                .collect(Collectors.toList());

        var matchingTransition = eligibleTransitions.stream()
                .filter(ticketStateTransition -> switch (ticketStateTransition.getType()) {
                    case EVALUATED -> ruleEngine.evaluate(ticketStateTransition.getRule(), evalDataJson);
                    case DEFAULT -> false;
                })
                .findFirst()
                .orElse(null);

        if (null == matchingTransition) {
            matchingTransition = eligibleTransitions.stream()
                    .filter(transition -> transition.getType()
                            .equals(TicketStateTransition.TicketStateTransitionType.DEFAULT))
                    .findAny()
                    .orElse(null);
        }
        return matchingTransition;
    }

}
