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
public class WorkflowSelectionRuleDeletedEvent extends Event {
    String workflowId;
    String ruleId;

    @Builder
    public WorkflowSelectionRuleDeletedEvent(String workflowId, String ruleId) {
        super(EventType.WORKFLOW_SELECTION_RULE_DELETED);
        this.workflowId = workflowId;
        this.ruleId = ruleId;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}