package io.appform.conductor.server.parser;

import io.appform.conductor.model.events.analytics.EventFilters;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.util.List;

public interface CQLEventFilterFunction {
    EventFilters.EventFiltersBuilder eventFilters(List<ExpressionList> parameters,
                                                  EventFilters.EventFiltersBuilder filtersBuilder);
}
