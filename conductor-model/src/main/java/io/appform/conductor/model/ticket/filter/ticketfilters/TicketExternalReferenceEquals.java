package io.appform.conductor.model.ticket.filter.ticketfilters;

import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.TicketFilterType;
import io.appform.conductor.model.ticket.filter.TicketFilterVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;


@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketExternalReferenceEquals extends TicketFilter {
    @NotNull
    @NotEmpty
    String source;

    @NotNull
    @NotEmpty
    String value;

    @Builder
    @Jacksonized
    public TicketExternalReferenceEquals(String source, String value) {
        super(TicketFilterType.EXTERNAL_REFERENCE_EQUALS);
        this.source = source;
        this.value = value;
    }

    @Override
    public <T> T accept(TicketFilterVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
