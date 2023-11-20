package io.appform.conductor.server.parser;

import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.server.utils.Pair;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.util.List;

public interface CQLFilterFunction {
    Pair<List<TicketFilter>, List<TicketFieldFilter>> ticketFilters(List<ExpressionList> parameters);
}
