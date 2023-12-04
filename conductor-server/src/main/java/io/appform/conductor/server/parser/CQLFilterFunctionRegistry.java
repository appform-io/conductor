package io.appform.conductor.server.parser;

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.events.analytics.EventFilters;
import io.appform.conductor.model.events.analytics.EventTimeWindow;
import io.appform.conductor.model.events.analytics.ObjectReference;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketExternalReferenceEquals;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.Pair;
import io.dropwizard.util.Duration;
import lombok.SneakyThrows;
import lombok.val;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;

import javax.inject.Singleton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class CQLFilterFunctionRegistry {

    private final Map<String, CQLTicketFilterFunction> ticketFilterFunctions = new ConcurrentHashMap<>();
    private final Map<String, CQLEventFilterFunction> eventFilterFunctions = new ConcurrentHashMap<>();

    public CQLFilterFunctionRegistry() {
        registerFilterFunctions();
    }

    public Optional<CQLTicketFilterFunction> ticketFilterFunction(String name) {
        return Optional.ofNullable(ticketFilterFunctions.get(name));
    }

    public Optional<CQLEventFilterFunction> eventFilterFunction(String name) {
        return Optional.ofNullable(eventFilterFunctions.get(name));
    }


    private void registerFilterFunctions() {
        ticketFilterFunctions.put("external_source_equals", CQLFilterFunctionRegistry::ticketExternalReferenceEquals);
        eventFilterFunctions.put("object_reference_equals", CQLFilterFunctionRegistry::objectReferenceEquals);
        eventFilterFunctions.put("last", CQLFilterFunctionRegistry::lastWindow);
    }

    @SuppressWarnings("rawtypes")
    private static Pair<List<TicketFilter>, List<TicketFieldFilter>> ticketExternalReferenceEquals(List<ExpressionList> parameters) {
        ConductorServerUtils.ensureCondition(parameters.size() == 2,
                                             ConductorErrorCode.CQL_INVALID_FUNCTION_PARAMETER,
                                             Map.of("functionName",
                                                    "external_source_equals"));
        return new Pair<>(
                List.of(new TicketExternalReferenceEquals(
                        fieldName(parameters.get(0)),
                        fieldName(parameters.get(1)))),
                List.of());
    }

    @SuppressWarnings("rawtypes")
    private static EventFilters.EventFiltersBuilder objectReferenceEquals(
            List<ExpressionList> parameters,
            EventFilters.EventFiltersBuilder filtersBuilder) {
        ConductorServerUtils.ensureCondition(parameters.size() == 2,
                                             ConductorErrorCode.CQL_INVALID_FUNCTION_PARAMETER,
                                             Map.of("functionName",
                                                    "object_reference_equals"));
        return filtersBuilder.reference(new ObjectReference(
                ReferredObjectType.valueOf(fieldName(parameters.get(0))),
                fieldName(parameters.get(1))));
    }

    @SneakyThrows
    @SuppressWarnings("rawtypes")
    private static EventFilters.EventFiltersBuilder lastWindow(
            List<ExpressionList> parameters,
            EventFilters.EventFiltersBuilder filtersBuilder) {
        val numParams = parameters.size();
        ConductorServerUtils.ensureCondition(numParams > 0 && numParams <=2,
                                             ConductorErrorCode.CQL_INVALID_FUNCTION_PARAMETER,
                                             Map.of("functionName",
                                                    "last"));
        val duration = Duration.parse(fieldName(parameters.get(0)));
        return filtersBuilder.timeWindow(EventTimeWindow.builder()
                                                 .duration(duration)
                                                 .from(numParams > 1
                                                       ? new SimpleDateFormat("yyyy-MM-dd").parse(
                                                         fieldName(parameters.get(1)))
                                                       : new Date())
                                                 .build());
    }

    private static String fieldName(Expression expression) {
        val accessedColName = new AtomicReference<String>();
        expression.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(Column column) {
                accessedColName.set(column.getFullyQualifiedName());
            }

            @Override
            public void visit(LongValue value) {
                accessedColName.set(value.getStringValue());
            }

            @Override
            public void visit(StringValue value) {
                accessedColName.set(value.getValue());
            }
        });
        return accessedColName.get();
    }


}
