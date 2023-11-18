package io.appform.conductor.server.eventmanagement.events.reporting;

import io.appform.conductor.model.reporting.ReportRunResult;
import io.appform.conductor.server.eventmanagement.Event;
import io.appform.conductor.server.eventmanagement.EventSubType;
import io.appform.conductor.server.eventmanagement.EventType;
import io.appform.conductor.server.eventmanagement.EventVisitor;
import io.appform.conductor.server.eventmanagement.events.ReferredObjectType;
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