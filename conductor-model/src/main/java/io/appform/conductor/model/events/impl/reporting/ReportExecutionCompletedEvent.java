package io.appform.conductor.model.events.impl.reporting;

import io.appform.conductor.model.reporting.ReportRunResult;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.EventVisitor;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventSubType(EventType.REPORT_EXECUTION_COMPLETED)
@SuperBuilder
@Jacksonized
public class ReportExecutionCompletedEvent extends Event {
    ReportRunResult result;

    public ReportExecutionCompletedEvent(String reportId, ReportRunResult result) {
        super(EventType.REPORT_EXECUTION_COMPLETED, ReferredObjectType.REPORT, reportId);
        this.result = result;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}