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



@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketsUpdatedBeforeTimeWindow extends TicketFilter {

    Duration duration;

    @Builder
    @Jacksonized
    public TicketsUpdatedBeforeTimeWindow(Duration duration) {
        super(TicketFilterType.UPDATED_BEFORE_TIME_WINDOW);
        this.duration = duration;
    }

    @Override
    public <T> T accept(TicketFilterVisitor<T> visitor) {
        return visitor.visit(this);
    }
}