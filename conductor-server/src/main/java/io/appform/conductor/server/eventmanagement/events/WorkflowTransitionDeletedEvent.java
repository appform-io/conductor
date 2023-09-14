package io.appform.conductor.server.eventmanagement.events;

import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;



@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkflowTransitionDeletedEvent extends Event {
    String workflowId;
    String transitionId;

    @Builder
    public WorkflowTransitionDeletedEvent(String workflowId, String transitionId) {
        super(EventType.WORKFLOW_TRANSITION_DELETED);
        this.workflowId = workflowId;
        this.transitionId = transitionId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}