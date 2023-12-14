package io.appform.conductor.model.events.impl.reporting;

import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.EventVisitor;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.reporting.ReportState;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EventSubType(EventType.REPORT_STATE_UPDATED)
@SuperBuilder
@Jacksonized
public class ReportStateUpdatedEvent extends Event {

    ReportState state;

    public ReportStateUpdatedEvent(String reportId, ReportState state) {
        super(EventType.REPORT_STATE_UPDATED, ReferredObjectType.REPORT, reportId);
        this.state = state;
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}