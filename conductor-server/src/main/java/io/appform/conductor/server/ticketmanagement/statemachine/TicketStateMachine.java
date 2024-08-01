package io.appform.conductor.server.ticketmanagement.statemachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.server.ruleengines.RuleEngine;
import io.appform.conductor.server.ticketmanagement.statemachine.models.TicketStateMachineContext;
import io.appform.conductor.server.ticketmanagement.statemachine.models.TriggerData;
import io.appform.conductor.server.ticketmanagement.statemachine.models.strategy.TriggerStrategy;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class TicketStateMachine {

    private final ObjectMapper mapper;
    private final RuleEngine ruleEngine; //TODO:fix this way to remove this from sm and use it from outside
    private final TransitionHandler executor;
    @Getter
    private TicketStateMachineContext context;
    private Set<String> transitionAuditLog;

    public TicketStateMachine(
            TicketStateMachineContext context,
            ObjectMapper mapper,
            RuleEngine ruleEngine,
            TransitionHandler executor) {
        this.mapper = mapper;
        this.ruleEngine = ruleEngine;
        this.executor = executor;
        this.context = context;
        this.transitionAuditLog = new HashSet<>();
    }

    public boolean trigger(TriggerData event, TriggerStrategy strategy) {
        val triggered = new AtomicBoolean(false);
        if (strategy == TriggerStrategy.EXECUTE_FIRST) {
            triggered.set(trigger(event));
        } else if (strategy == TriggerStrategy.EXECUTE_ALL) {
            while (trigger(event)) {
                triggered.set(true);
                log.info("Triggering next transition for ticket: {}", this.context.getTicketSkeleton().getTicketId());
            }
        }
        return triggered.get();
    }

    private boolean trigger(TriggerData event) {
        TicketState currentState = this.context.currentState();
        if(currentState.isTerminal()) {
            log.info("Ticket {} is already in terminal state {}",
                    this.context.ticketId(),
                    currentState.getDisplayName());
            return true;
        }
        val transition = selectTransition(event);
        if (transition == null) {
            log.info("No possible transitions found for ticket: {}. Will stop state machine execution.",
                    this.context.ticketId());
            return false;
        }
        log.debug("Found transition: {}", transition);
        beforeTransition(transition, this.context, event);
        onTransition(transition, this.context, event);
        afterTransition(transition, this.context, event);
        return false;
    }

    private void beforeTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData event) {
        executor.beforeTransition(transition, context, event);
    }


    private void onTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData event) {
        executor.onTransition(transition, context, event);
    }


    private void afterTransition(TicketStateTransition transition, TicketStateMachineContext context, TriggerData event) {
        executor.afterTransition(transition, context, event);
        this.transitionAuditLog.add(transition.getId());
    }

    private TicketStateTransition selectTransition(TriggerData event) {
        //TODO: fix move this out along with mapper and schema
        val currentState = this.context.currentState();
        val evalDataJson = ConductorServerUtils.evalDataJson(mapper, this.context, event.getPayload());

        log.debug("Evaluating transitions for ticket: {} with data: {}",
                this.context.ticketId(),
                evalDataJson);

        //filter out transitions already transitioned to avoid loops
        List<TicketStateTransition> eligibleTransitions =  context.getWorkflow()
                .getTicketStateTransitions()
                .getOrDefault(currentState.getId(), List.of())
                .stream()
                .filter(transition -> !transitionAuditLog.contains(transition.getId()))
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
