package io.appform.conductor.model.events.impl.task;

import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.EventVisitor;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventSubType(EventType.TASK_UPDATED)
@SuperBuilder
@Jacksonized
public class TaskUpdatedEvent extends Event {

    public TaskUpdatedEvent(String taskId) {
        super(EventType.TASK_UPDATED, ReferredObjectType.TASK, taskId);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
