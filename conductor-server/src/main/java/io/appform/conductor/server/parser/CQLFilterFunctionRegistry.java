package io.appform.conductor.server.parser;

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.ticket.filter.ticketfilters.TicketExternalReferenceEquals;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.Pair;
import lombok.val;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.schema.Column;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class CQLFilterFunctionRegistry {

    private final Map<String, CQLFilterFunction> registry;

    public CQLFilterFunctionRegistry() {
        this.registry = new ConcurrentHashMap<>();
        registerFilterFunctions();
    }

    public Optional<CQLFilterFunction> filterFunction(String name) {
        return Optional.ofNullable(registry.get(name));
    }


    private void registerFilterFunctions() {
        registry.put("external_source_equals",
                parameters -> {
                    ConductorServerUtils.ensureCondition(parameters.size() == 2,
                            ConductorErrorCode.CQL_INVALID_FUNCTION_PARAMETER,
                            Map.of("functionName", "external_source_equals"));
                    return new Pair<>(
                            List.of(new TicketExternalReferenceEquals(
                                    fieldName(parameters.get(0)),
                                    fieldName(parameters.get(1)))),
                            List.of());
                });
    }

    private String fieldName(Expression expression) {
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
