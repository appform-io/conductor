package io.appform.conductor.model.events.impl.reporting;

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
@EventSubType(EventType.REPORT_DELETED)
@SuperBuilder
@Jacksonized
public class ReportDeletedEvent extends Event {

    public ReportDeletedEvent(String reportId) {
        super(EventType.REPORT_DELETED, ReferredObjectType.REPORT, reportId);
    }

    @Override
    public <T> T accept(EventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}