package io.appform.conductor.model.ticket.filter.ticketfilters;

import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.TicketFilterType;
import io.appform.conductor.model.ticket.filter.TicketFilterVisitor;
import io.dropwizard.util.Duration;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Date;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketsUpdatedBeforeTimeWindow extends TicketFilter {

    Date start;
    Duration duration;

    @Builder
    @Jacksonized
    public TicketsUpdatedBeforeTimeWindow(Duration duration, Date start) {
        super(TicketFilterType.UPDATED_BEFORE_TIME_WINDOW);
        this.duration = duration;
        this.start = start;
    }

    @Override
    public <T> T accept(TicketFilterVisitor<T> visitor) {
        return visitor.visit(this);
    }
}