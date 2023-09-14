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
public class WorkflowStateDeletedEvent extends Event {
    String workflowId;
    String stateId;

    @Builder
    public WorkflowStateDeletedEvent(String workflowId, String stateId) {
        super(EventType.WORKFLOW_STATE_DELETED);
        this.workflowId = workflowId;
        this.stateId = stateId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}