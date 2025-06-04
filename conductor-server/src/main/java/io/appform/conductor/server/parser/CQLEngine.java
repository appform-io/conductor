/*
 * Copyright (c) 2023 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.parser;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.analytics.EventFilters;
import io.appform.conductor.model.events.analytics.EventQueryOpCode;
import io.appform.conductor.model.events.analytics.EventQueryResponse;
import io.appform.conductor.model.events.analytics.EventTimeWindow;
import io.appform.conductor.model.events.analytics.impl.EventGroupRequest;
import io.appform.conductor.model.events.analytics.impl.EventListRequest;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.model.ticket.filter.Filters;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.fieldfilters.*;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.eventmanagement.EventStore;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.ticketmanagement.TicketSkeleton;
import io.appform.conductor.server.utils.Pair;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import io.dropwizard.util.Duration;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.lang3.Range;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.appform.conductor.server.utils.ConductorServerUtils.lowerSnake;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
public class CQLEngine {
    private static final Map<String, Class<?>> KNOWN_TICKET_ATTRIBUTES
            = Arrays.stream(TicketSkeleton.class.getDeclaredFields())
            .collect(Collectors.toMap(Field::getName, Field::getType));


    private static final Map<String, Class<?>> KNOWN_TICKET_DATE_ATTRIBUTES
            = Maps.filterEntries(KNOWN_TICKET_ATTRIBUTES, field -> field.getValue().equals(Date.class));

    private static final Map<String, Class<?>> KNOWN_EVENTS_ATTRIBUTES
            = Stream.concat(Arrays.stream(Event.class.getDeclaredFields()),
                            Arrays.stream(Event.EventTime.class.getDeclaredFields()))
            .collect(Collectors.toMap(Field::getName, Field::getType));
    private static final Map<String, Class<?>> KNOWN_EVENT_DATE_ATTRIBUTES
            = Maps.filterEntries(KNOWN_EVENTS_ATTRIBUTES, field -> field.getValue().equals(Date.class));

    private static final Set<FieldType> COMPARABLE_TICKET_FIELD_TYPES = EnumSet.of(FieldType.DATE, FieldType.NUMBER);
    private static final Set<? extends Class<? extends Number>> NUMERIC_JAVA_TYPES
            = Set.of(Long.class, Integer.class, Double.class);

    private static final Set<String> KNOWN_META_FIELDS = Set.of();
    private static final Set<String> KNOWN_FUNCTIONS = Set.of();

    private static final String TICKETS_DB_PREFIX = "tickets.";
    private static final String EVENTS_DB = "events";
    private static final String TICKETS_FIELDS_PREFIX = "fields.";

    private final WorkflowStore workflowStore;
    private final SchemaStore schemaStore;
    private final CQLFilterFunctionRegistry cqlFilterFunctionRegistry;

    public enum QueryDomain {
        TICKETS,
        EVENTS
    }

    @Data
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static abstract class CQLParserOutput {
        private final QueryDomain domain;
        private final List<GroupingElement> groupingElements;

        public abstract <T> T accept(final CQLParserOutputVisitor<T> visitor);
    }

    public interface CQLParserOutputVisitor<T> {

        T visit(final CQLTicketParserOutput ticketParserOutput);

        T visit(final CQLEventParserOutput eventParserOutput);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class CQLTicketParserOutput extends CQLParserOutput {
        private final Filters filters;
        private final List<SelectedField> selectedFields;
        private final TicketQueryOpCode opCode;
        private final TimeSeriesDetails timeSeriesDetails;
        private final long limit;

        private CQLTicketParserOutput(
                Filters filters,
                List<SelectedField> selectedFields,
                List<GroupingElement> groupingElements,
                TicketQueryOpCode opCode,
                TimeSeriesDetails timeSeriesDetails,
                long limit) {
            super(QueryDomain.TICKETS, groupingElements);
            this.filters = filters;
            this.selectedFields = selectedFields;
            this.opCode = opCode;
            this.timeSeriesDetails = timeSeriesDetails;
            this.limit = limit;
        }

        @Override
        public <T> T accept(CQLParserOutputVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class CQLEventParserOutput extends CQLParserOutput {
        private final EventQueryOpCode opCode;
        private final EventFilters filters;
        private final long limit;

        private CQLEventParserOutput(
                EventQueryOpCode opCode,
                EventFilters filters,
                List<GroupingElement> groupingElements,
                long limit) {
            super(QueryDomain.EVENTS, groupingElements);
            this.opCode = opCode;
            this.filters = filters;
            this.limit = limit;
        }

        @Override
        public <T> T accept(CQLParserOutputVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    @Value
    @Builder
    public static class CQLQueryExecutionOutput {
        QueryDomain domain;
        TicketQueryResponse ticketQueryResponse;
        EventQueryResponse eventQueryResponse;
    }

    public static CQLQueryExecutionOutput runQuery(
            String requestId,
            String next,
            int size,
            CQLParserOutput parserOutput,
            TicketManager ticketManager,
            EventStore eventStore) {
        return parserOutput.accept(new CQLParserOutputVisitor<>() {
            @Override
            public CQLQueryExecutionOutput visit(CQLTicketParserOutput ticketParserOutput) {
                val filters = Objects.requireNonNullElse(ticketParserOutput.getFilters(), Filters.EMPTY); //TODO::THROW
                val ticketRequest = switch (ticketParserOutput.getOpCode()) {

                    case LIST -> TicketListRequest.builder()
                            .queryId(requestId)
                            .filters(filters)
                            .direction(TicketListRequest.Direction.FORWARD)
                            .ticketDataFields(Objects.requireNonNullElse(ticketParserOutput.getSelectedFields(),
                                                                         List.<SelectedField>of())
                                                      .stream()
                                                      .map(SelectedField::fieldSchemaId)
                                                      .toList())
                            .next(next)
                            .size(Math.min(200, Math.max(size, (int) ticketParserOutput.getLimit())))
                            .build();
                    case GROUP -> TicketGroupRequest.builder()
                            .queryId(requestId)
                            .filters(filters)
                            .groupingFields(ticketParserOutput.getGroupingElements())
                            .build();
                };
                log.info("Running query with request id: {}. Query: {}", requestId, ticketRequest);
                return CQLQueryExecutionOutput.builder()
                        .domain(QueryDomain.TICKETS)
                        .ticketQueryResponse(ticketManager.query(ticketRequest))
                        .build();
            }

            @Override
            public CQLQueryExecutionOutput visit(CQLEventParserOutput eventParserOutput) {
                val filter = eventParserOutput.getFilters();
                val query = switch (eventParserOutput.opCode) {

                    case LIST -> new EventListRequest(requestId, filter, next, 10);
//                    case LIST -> new EventListRequest(requestId, filter, next, Math.min(200, Math.max(size, (int) eventParserOutput.getLimit())));
                    case GROUP_BY -> new EventGroupRequest(requestId, filter, eventParserOutput.getGroupingElements());
                };
                return CQLQueryExecutionOutput.builder()
                        .domain(QueryDomain.EVENTS)
                        .eventQueryResponse(eventStore.query(query))
                        .build();
            }
        });

    }

    public record TimeSeriesDetails(TimeResolution resolution, String column) {
        public static final TimeSeriesDetails DEFAULT = new TimeSeriesDetails(TimeResolution.DAY,
                                                                              TicketSkeleton.Fields.updated);
    }

    @SneakyThrows
    public Optional<CQLParserOutput> parse(final String query) {
        if (Strings.isNullOrEmpty(query)) {
            return Optional.empty();
        }
        val stmt = CCJSqlParserUtil.parse(query);
        val select = toPlainSelect(stmt).orElse(null);
        Preconditions.checkNotNull(select, "Only select statements are supported");
        val queryDomain = determineQueryDomain(select);
        return switch (queryDomain) {

            case TICKETS -> parseTicketWorkflowCQL(select);
            case EVENTS -> parseEventsQuery(select);
        };
    }

    private Optional<CQLParserOutput> parseEventsQuery(PlainSelect select) {
        val groupParsingOutput = parseGroupExpression(
                select,
                (colName, groupByExpr) -> {
                    Preconditions.checkArgument(
                            KNOWN_EVENTS_ATTRIBUTES.containsKey(colName),
                            "Only event attributes " + KNOWN_EVENTS_ATTRIBUTES.keySet()
                                    + " are allowed in group by clause. Expr: " + groupByExpr);
                });
        return Optional.of(new CQLEventParserOutput(
                switch (groupParsingOutput.groupOpType.get()) {
                    case LIST -> EventQueryOpCode.LIST;
                    case GROUP -> EventQueryOpCode.GROUP_BY;
                },
                parseEventFilters(select),
                groupParsingOutput.groupingElements,
                100));
    }

    private Optional<CQLParserOutput> parseTicketWorkflowCQL(PlainSelect select) {
        val workflowId = ticketWorkFlowId(select);
        val wf = workflowStore.read(workflowId).orElse(null);
        Preconditions.checkNotNull(wf, "Invalid workflow id");
        log.debug("Will proceed with workflow: {}", wf.getDisplayName());
        val schema = schemaStore.read(wf.getSchemaId()).orElse(null);
        Preconditions.checkNotNull(schema, "Invalid schema for workflow " + workflowId);
        val fieldSchema = Objects.requireNonNullElse(schema.getFields(), List.<FieldSchema>of())
                .stream()
                .collect(Collectors.toMap(FieldSchema::getName, java.util.function.Function.identity()));
        val selectedFields = selectedFields(select, fieldSchema);
        log.debug("Fields selected: {}", selectedFields);
        val filters = parseTicketFilters(workflowId, select, fieldSchema);
        val groupParsingOutput = parseGroupExpression(
                select,
                (colName, groupByExpr) -> {
                    val elementType = elementType(colName, fieldSchema).orElse(null);
                    Preconditions.checkArgument(
                            elementType == ElementType.TICKET_ATTRIBUTE
                                    && KNOWN_TICKET_ATTRIBUTES.containsKey(colName),
                            "Only ticket attributes " + KNOWN_TICKET_ATTRIBUTES.keySet()
                                    + " are allowed in group by clause. Expr: " + groupByExpr);
                });
        val limit = new AtomicLong(0);
        val limitExpr = select.getLimit();
        if (null != limitExpr) {
            limitExpr.getRowCount().accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(LongValue value) {
                    Preconditions.checkArgument(value.getValue() > 0 && value.getValue() < Integer.MAX_VALUE,
                                                "Limit must be in int range. Expr: " + limitExpr);
                    limit.set(value.getValue());
                }
            });
        }
        return Optional.of(new CQLTicketParserOutput(filters,
                                                     selectedFields.getSecond(),
                                                     groupParsingOutput.groupingElements(),
                                                     groupParsingOutput.groupOpType().get(),
                                                     groupParsingOutput.timeSeriesDetails().get(),
                                                     limit.get()));
    }

    @SuppressWarnings("unchecked")
    private GroupParsingOutput parseGroupExpression(
            PlainSelect select,
            BiConsumer<String, GroupByElement> groupColValidator) {
        val groupingElements = new ArrayList<GroupingElement>();
        val groupByExpr = select.getGroupBy();
        val groupOpType = new AtomicReference<>(TicketQueryOpCode.LIST);
        val timeSeriesDetails = new AtomicReference<TimeSeriesDetails>(null);
        if (null != groupByExpr) {
            groupOpType.set(TicketQueryOpCode.GROUP);
            groupByExpr.getGroupByExpressionList()
                    .forEach(expr -> ((Expression) expr).accept(new ExpressionVisitorAdapter() {
                        @Override
                        public void visit(Function function) {
                            Preconditions.checkArgument(function.getName().equals("time_bucket"),
                                                        "Only 'time_bucket' function is allowed in group by");
//                            groupOpType.set(TicketQueryOpCode.TIME_SERIES);
                            val paramCount = null == function.getParameters()
                                             ? 0
                                             : function.getParameters().size();
                            val colName = switch (paramCount) {
                                case 1, 2 -> {
                                    val dateCol = fieldName(((Expression) function.getParameters().get(0)));
                                    groupColValidator.accept(dateCol, groupByExpr);
                                    yield dateCol;
                                }
                                default -> TicketSkeleton.Fields.updated;
                            };
                            val resolution = (paramCount > 1)
                                             ? TimeResolution.valueOf(((StringValue) function.getParameters()
                                    .get(1)).getValue())
                                             : TimeResolution.DAY;
//                            timeSeriesDetails.set(new TimeSeriesDetails(resolution, colName));
                            groupingElements.add(new TimeBucketGroupingElement(colName, resolution, colName));
                        }

                        @Override
                        public void visit(Column column) {
                            val colName = column.getFullyQualifiedName();
                            groupColValidator.accept(colName, groupByExpr);
                            groupingElements.add(new ColumnGroupingElement(colName, colName));
                        }
                    }));
        }
        return new GroupParsingOutput(groupingElements, groupOpType, timeSeriesDetails);
    }

    private record GroupParsingOutput(List<GroupingElement> groupingElements,
                                      AtomicReference<TicketQueryOpCode> groupOpType,
                                      AtomicReference<TimeSeriesDetails> timeSeriesDetails) {
    }


    private static QueryDomain determineQueryDomain(final PlainSelect select) {
        val queryDomain = new AtomicReference<QueryDomain>();
        select.getFromItem().accept(new FromItemVisitorAdapter() {
            @Override
            public void visit(Table table) {
                val fqtn = table.getFullyQualifiedName();
                log.debug("Query table name = {}", fqtn);
                if (fqtn.startsWith(TICKETS_DB_PREFIX)) {
                    queryDomain.set(QueryDomain.TICKETS);
                }
                else if (fqtn.equals(EVENTS_DB)) {
                    queryDomain.set(QueryDomain.EVENTS);
                }
            }
        });
        Preconditions.checkNotNull(
                queryDomain.get(),
                "From should be 'tickets.workflowId' or 'events'. Expr: " + select.getFromItem().toString());
        return queryDomain.get();
    }

    private String ticketWorkFlowId(final PlainSelect select) {
        val wfId = new AtomicReference<String>();
        select.getFromItem().accept(new FromItemVisitorAdapter() {

            @Override
            public void visit(Table table) {
                val fqtn = table.getFullyQualifiedName();
                log.debug("Query table name = {}", fqtn);
                Preconditions.checkArgument(fqtn.startsWith(TICKETS_DB_PREFIX),
                                            "From should be 'tickets.workflowId'. Expr: " + table.getFullyQualifiedName());
                val namespaceIdx = fqtn.indexOf(".");
                val workflowId = fqtn.substring(namespaceIdx + 1);

                Preconditions.checkArgument(!Strings.isNullOrEmpty(workflowId),
                                            "Please use tickets.<workflowId> in 'from' clause");
                wfId.set(workflowId);
            }
        });
        return wfId.get();
    }

    private Optional<PlainSelect> toPlainSelect(Statement statement) {
        val retValue = new AtomicReference<PlainSelect>();
        statement.accept(new StatementVisitorAdapter() {
            @Override
            public void visit(Select select) {
                select.getPlainSelect().accept(new SelectVisitorAdapter() {
                    @Override
                    public void visit(PlainSelect plainSelect) {
                        retValue.set(plainSelect);
                    }
                });
            }
        });
        return Optional.ofNullable(retValue.get());
    }

    public record SelectedField(String fieldSchemaId, String name) {
    }

    private Pair<List<String>, List<SelectedField>> selectedFields(
            final PlainSelect plainSelect, Map<String, FieldSchema> fieldSchema) {
        val ticketAttributes = new ArrayList<String>();
        val ticketFields = new ArrayList<SelectedField>();

        plainSelect.accept(new SelectVisitorAdapter() {

            @Override
            public void visit(PlainSelect plainSelect) {
                plainSelect.getSelectItems().forEach(item -> item.getExpression()
                        .accept(new ExpressionVisitorAdapter() {
                            @Override
                            public void visit(AllColumns allColumns) {
                                if (allColumns instanceof AllTableColumns allTableColumns
                                        && allTableColumns.getTable().getFullyQualifiedName().equals("fields")) {
                                    ticketFields.addAll(
                                            fieldSchema.values()
                                                    .stream()
                                                    .map(field -> new SelectedField(field.getId(), field.getName()))
                                                    .toList());
                                }
                            }

                            @Override
                            public void visit(Column column) {

                                val fieldName = fieldName(column);
                                if (!KNOWN_TICKET_ATTRIBUTES.containsKey(fieldName)) {
                                    val field = fieldSchema.get(fieldName);
                                    Preconditions.checkNotNull("Unkown ticket field selected. Field: " + fieldName);
                                    ticketFields.add(new SelectedField(field.getId(), fieldName));
                                }
                                else {
                                    ticketAttributes.add(fieldName);
                                }
                            }
                        }));
            }
        });
        return Pair.of(ticketAttributes, ticketFields);
    }

    private String fieldName(Expression expression) {
        val accessedColName = new AtomicReference<String>();
        expression.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(Column column) {
                val colName = column.getFullyQualifiedName();
                if (colName.startsWith(TICKETS_FIELDS_PREFIX)) {
                    val fieldName = fieldName(colName);
                    Preconditions.checkNotNull(fieldName,
                                               "Unknown ticket field name: " + fieldName);
                    accessedColName.set(fieldName);
                }
                else {
                    if (KNOWN_TICKET_ATTRIBUTES.containsKey(colName)) {
                        accessedColName.set(colName);
                    }
                    else if (KNOWN_EVENTS_ATTRIBUTES.containsKey(colName)) {
                        accessedColName.set(colName);
                    }
                    else {
                        throw new IllegalArgumentException(
                                "Unknown attribute selected. Attribute: " + colName);
                    }
                }
                log.debug("Added selection field: {}", colName);
            }
        });
        return accessedColName.get();
    }


    private Object ticketExpressionValue(
            Expression expression,
            ElementType elementType,
            FieldSchema fieldSchema,
            Set<Class<?>> ticketAttrType) {
        val response = new AtomicReference<>();
        expression.accept(new ExpressionVisitorAdapter() {

            @Override
            @SneakyThrows
            public void visit(StringValue value) {
                if (elementType == ElementType.TICKET_ATTRIBUTE) {
                    if (ticketAttrType.contains(String.class) || ticketAttrType.contains(Enum.class)) {
                        response.set(value.getValue());
                    }
                    else {
                        throw new IllegalArgumentException(
                                "Expected value of type " + ticketAttrType + " for ticket attribute");
                    }
                }
                else {
                    response.set(switch (fieldSchema.getType()) {
                        case STRING, CHOICE -> value.getValue();
                        case BOOLEAN -> Boolean.parseBoolean(value.getValue());
                        case NUMBER -> Double.parseDouble(value.getValue());
                        case DATE -> new SimpleDateFormat("yyyy-MM-dd").parse(value.getValue());
                        default ->
                                throw new IllegalArgumentException("Required value of type " + fieldSchema.getType());
                    });
                }

            }


            @Override
            public void visit(DateValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Date.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.DATE)) {
                    response.set(value.getValue());
                }
                else {
                    throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
                }
            }

            @Override
            public void visit(DoubleValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Double.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.NUMBER)) {
                    response.set(value.getValue());
                }
                else {
                    throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
                }
            }

            @Override
            public void visit(LongValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Double.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.NUMBER)) {
                    response.set((double) value.getValue());
                }
                else {
                    throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
                }
            }


            @Override
            public void visit(TimeValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Date.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.DATE)) {
                    response.set(value.getValue());
                }
                else {
                    throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
                }
            }

            @Override
            public void visit(TimestampValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Date.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.DATE)) {
                    response.set(value.getValue());
                }
                else {
                    throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
                }
            }
        });
        Preconditions.checkNotNull(response.get(), "Please provide value in rhs of where clause");
        return response.get();
    }

    private Object eventExpressionValue(String attributeName, Expression expression, Class<?> type) {
        val response = new AtomicReference<>();
        expression.accept(new ExpressionVisitorAdapter() {

            @Override
            @SneakyThrows
            public void visit(StringValue value) {
                if (type == String.class || type == Enum.class) {
                    response.set(value.getValue());
                }
                else if (type == Boolean.class) {
                    response.set(Boolean.parseBoolean(value.getValue()));
                }
                else if (type == Long.class) {
                    response.set(Long.parseLong(value.getValue()));
                }
                else if (type == Integer.class) {
                    response.set(Integer.parseInt(value.getValue()));
                }
                else if (type == Date.class) {
                    try {
                        new SimpleDateFormat("yyyy-MM-dd").parse(value.getValue());
                    }
                    catch (ParseException e) {
                        log.trace("Did not match yyyy-MM-dd");
                    }
                    response.set(new Date(Long.parseLong(value.getValue())));
                }
            }


            @Override
            public void visit(DateValue value) {
                if (type == Date.class) {
                    response.set(value.getValue());
                }
            }

            @Override
            public void visit(DoubleValue value) {
                if (type == Double.class) {
                    response.set(value.getValue());
                }
            }

            @Override
            public void visit(LongValue value) {
                if (type == Long.class) {
                    response.set((double) value.getValue());
                }
            }


            @Override
            public void visit(TimeValue value) {
                if (type == Date.class) {
                    response.set(value.getValue());
                }
            }

            @Override
            public void visit(TimestampValue value) {
                if (type == Date.class) {
                    response.set(value.getValue());
                }
            }
        });
        Preconditions.checkNotNull(response.get(),
                                   "Please provide value in rhs of where clause. Expected value type: " + type.getSimpleName());
        return response.get();
    }

    private EventFilters parseEventFilters(final PlainSelect select) {
        val filtersBuilder = EventFilters.builder();
        val whereClause = select.getWhere();
        if (null == whereClause) {
            return filtersBuilder.build();
        }
        whereClause.accept(new ExpressionVisitorAdapter() {

            @Override
            @SuppressWarnings("unchecked")
            public void visit(Function function) {
                val functionName = function.getName();
                cqlFilterFunctionRegistry.eventFilterFunction(functionName)
                        .ifPresentOrElse(registeredFunction -> registeredFunction.eventFilters(function.getParameters(),
                                                                                               filtersBuilder),
                                         () -> super.visit(function));
            }

            @Override
            public void visit(Between expr) {
                val name = fieldName(expr.getLeftExpression());
                val elementType = elementType(name, Map.of()).orElse(null);
                val type = KNOWN_EVENTS_ATTRIBUTES.get(name);

                Preconditions.checkNotNull(elementType, "Element type in between expression could not be determined");
                ensureEventAttribute(elementType);

                val startValue = eventExpressionValue(name, expr.getBetweenExpressionStart(), type);
                val endValue = eventExpressionValue(name, expr.getBetweenExpressionEnd(), type);
                switch (name) {
                    case Event.Fields.date -> {
                        val from = readDateValue(name, startValue);
                        val to = readDateValue(name, endValue);
                        filtersBuilder.timeWindow(EventTimeWindow.builder()
                                                          .from(from.before(to) ?  from
                                                                                : to)
                                                          .duration(Duration.milliseconds(Math.abs(from.getTime() - to.getTime())))
                                                          .build());
                    }
                    case Event.EventTime.Fields.day -> {
                        val start = readLongValue(name, startValue);
                        val end = readLongValue(name, endValue);
                        val from = Math.min(start, end);
                        val to = Math.max(start, end);
                        Preconditions.checkArgument(from > 0 && from < 31 && to > 0 && to < 31,
                                                    "Day values can be from 1-31 only");
                        filtersBuilder.dayRange(Range.between(from, to));
                    }
                    //TODO::OTHER NUMBER RANGES FOR EVENT TIME
                    default ->
                            throw new IllegalArgumentException("Between operator is not applicable for field: " + name + ". Expr: " + expr);
                }

            }

            record ComparisonParsedOutput(
                    String name,
                    ElementType elementType,
                    Object value,
                    Class<?> attributeType
            ) {
            }

            @Override
            public void visit(EqualsTo expr) {
                val parsed = parseComparisonOperator(expr);
                switch (parsed.name) {
                    case Event.Fields.type -> filtersBuilder.eventType(EventType.valueOf(parsed.value.toString()));
                    case Event.Fields.objectType ->
                            filtersBuilder.referenceType(ReferredObjectType.valueOf(parsed.value.toString()));
                    case Event.Fields.userId -> filtersBuilder.userId(parsed.value.toString());
                    case Event.EventTime.Fields.day -> {
                        val value = readLongValue(parsed.name, parsed.value);
                        filtersBuilder.dayRange(Range.between(value, value));
                    }
                    //TODO::FOR OTHER RANGE TYPE VALUE
                    default -> throw new IllegalArgumentException(
                            "Equals to operator is not applicable for field: " + parsed.name + ". Expr: " + expr);
                }
            }

            @Override
            public void visit(NotEqualsTo expr) {
                val parsed = parseComparisonOperator(expr);

                switch (parsed.name) {
                    default -> throw new IllegalArgumentException(
                            "Equals to operator is not applicable for events field: " + parsed.name + ". Expr: " + expr);
                }
            }

            @Override
            public void visit(GreaterThan expr) {
                val parsed = parseComparisonOperator(expr);
                switch (parsed.name) {
                    case Event.EventTime.Fields.day -> {
                        val value = readLongValue(parsed.name, parsed.value);
                        filtersBuilder.dayRange(Range.between(value + 1, Long.MAX_VALUE));
                    }
                    //TODO::FOR OTHER RANGE TYPE VALUE
                    default -> throw new IllegalArgumentException(
                            "Greater than operator is not applicable for field: " + parsed.name + ". Expr: " + expr);
                }
            }

            @Override
            public void visit(GreaterThanEquals expr) {
                val parsed = parseComparisonOperator(expr);
                switch (parsed.name) {
                    case Event.EventTime.Fields.day -> {
                        val value = readLongValue(parsed.name, parsed.value);
                        filtersBuilder.dayRange(Range.between(value, Long.MAX_VALUE));
                    }
                    //TODO::FOR OTHER RANGE TYPE VALUE
                    default -> throw new IllegalArgumentException(
                            "Greater than equals operator is not applicable for field: " + parsed.name + ". Expr: " + expr);
                }

            }

            @Override
            public void visit(InExpression expr) {
                val name = fieldName(expr.getLeftExpression());
                val values = new ArrayList<>();
                expr.getRightExpression().accept(new ExpressionVisitorAdapter() {

                    @Override
                    public void visit(ExpressionList<?> expressionList) {
                        expressionList.forEach(item -> values.add(readValue(item)));
                    }
                });
                switch (name) {
                    case Event.Fields.type -> filtersBuilder.eventTypes(
                            values.stream()
                                    .map(v -> EventType.valueOf(v.toString()))
                                    .collect(Collectors.toUnmodifiableSet()));
                    case Event.Fields.userId -> filtersBuilder.userIds(
                            values.stream()
                                    .map(Object::toString)
                                    .collect(Collectors.toUnmodifiableSet()));
                    default -> throw new IllegalArgumentException(
                            "In operator is not applicable for field: " + name + ". Expr: " + expr);
                }
            }

            @Override
            public void visit(MinorThan expr) {
                val parsed = parseComparisonOperator(expr);
                switch (parsed.name) {
                    case Event.EventTime.Fields.day -> {
                        val value = readLongValue(parsed.name, parsed.value);
                        filtersBuilder.dayRange(Range.between(Long.MIN_VALUE, value - 1));
                    }
                    //TODO::FOR OTHER RANGE TYPE VALUE
                    default -> throw new IllegalArgumentException(
                            "Less than operator is not applicable for field: " + parsed.name + ". Expr: " + expr.getStringExpression());
                }
            }

            @Override
            public void visit(MinorThanEquals expr) {
                val parsed = parseComparisonOperator(expr);
                switch (parsed.name) {
                    case Event.EventTime.Fields.day -> {
                        val value = readLongValue(parsed.name, parsed.value);
                        filtersBuilder.dayRange(Range.between(Long.MIN_VALUE, value));
                    }
                    //TODO::FOR OTHER RANGE TYPE VALUE
                    default -> throw new IllegalArgumentException(
                            "Less than equals is not applicable for field: " + parsed.name + ". Expr: " + expr.getStringExpression());
                }
            }

            private ComparisonParsedOutput parseComparisonOperator(final ComparisonOperator expr) {
                var name = fieldName(expr.getLeftExpression());
                Preconditions.checkArgument(!Strings.isNullOrEmpty(name),
                                            "event attribute is mandatory for comparison operator in " +
                                                    "while clause. Expr: " + expr.getStringExpression());
                val elementType = elementType(name, Map.of()).orElse(null);
                Preconditions.checkArgument(elementType == ElementType.EVENT_ATTRIBUTE,
                                            "Name can only be event attribute in comparison operator" +
                                                    ". Error exp: " + expr.getStringExpression());
                name = Strings.isNullOrEmpty(name) ? fieldName(expr.getRightExpression()) : name;

                val value = readValue(expr.getRightExpression());
                Preconditions.checkNotNull(value,
                                           "Value is mandatory for comparison operator. Expr: " + expr.getStringExpression());
                return new ComparisonParsedOutput(name, elementType, value, value.getClass());
            }

            private Object readValue(Expression expression) { //TODO::PARSE TO REQUIRED TYPE HERE ITSELF
                val response = new AtomicReference<>();
                expression.accept(new ExpressionVisitorAdapter() {

                    @Override
                    @SneakyThrows
                    public void visit(StringValue value) {
                        response.set(value.getValue());
                    }


                    @Override
                    public void visit(DateValue value) {
                        response.set(value.getValue());
                    }

                    @Override
                    public void visit(DoubleValue value) {
                        response.set(value.getValue());
                    }

                    @Override
                    public void visit(LongValue value) {
                        response.set(value.getValue());
                    }


                    @Override
                    public void visit(TimeValue value) {
                        response.set(Date.from(value.getValue().toInstant()));
                    }

                    @Override
                    public void visit(TimestampValue value) {
                        response.set(Date.from(value.getValue().toInstant()));
                    }
                });
                Preconditions.checkNotNull(response.get(), "Please provide value in rhs of where clause");
                return response.get();
            }

            private long readLongValue(String name, Object value) {
                if (NUMERIC_JAVA_TYPES.contains(KNOWN_EVENTS_ATTRIBUTES.get(name))) {
                    return (long) Double.parseDouble(value.toString());
                }
                throw new IllegalArgumentException("Could not parse date/numeric value for: " + name);
            }

            private Date readDateValue(String name, Object value) {
                if (KNOWN_EVENT_DATE_ATTRIBUTES.containsKey(name)) {
                    return (Date) value;
                }
                throw new IllegalArgumentException("Could not parse date/numeric value for: " + name);
            }
        });

        return filtersBuilder.build();
    }

    private Filters parseTicketFilters(
            String workflowId,
            final PlainSelect select,
            final Map<String, FieldSchema> schema) {
        val tfs = new ArrayList<TicketFilter>();
        val ffs = new ArrayList<TicketFieldFilter>();
        tfs.add(new TicketWorkflowEquals(workflowId));
        val response = new Filters(tfs, ffs);
        val whereClause = select.getWhere();
        if (null == whereClause) {
            return response;
        }
        whereClause.accept(new ExpressionVisitorAdapter() {

            @Override
            @SuppressWarnings("unchecked")
            public void visit(Function function) {
                val functionName = function.getName();
                cqlFilterFunctionRegistry.ticketFilterFunction(functionName)
                        .ifPresentOrElse(registeredFunction -> {
                                             Pair<List<TicketFilter>, List<TicketFieldFilter>> filters =
                                                     registeredFunction.ticketFilters(function.getParameters());
                                             tfs.addAll(filters.getFirst());
                                             ffs.addAll(filters.getSecond());
                                         },
                                         () -> super.visit(function));
            }

            @Override
            public void visit(Between expr) {
                val name = fieldName(expr.getLeftExpression());
                val elementType = elementType(name, schema).orElse(null);
                Preconditions.checkNotNull(elementType, "Element type in between expression could not be determined");
                ensureTicketAttributeOrTicketField(elementType);

                val fieldSchema = schema.get(name);
                switch (elementType) {

                    case TICKET_ATTRIBUTE -> Preconditions.checkNotNull(KNOWN_TICKET_ATTRIBUTES.get(name),
                                                            "Unknown ticket attribute");
                    case TICKET_FIELD -> {
                        Preconditions.checkNotNull(fieldSchema, "Invalid field name " + name);
                        Preconditions.checkArgument(COMPARABLE_TICKET_FIELD_TYPES.contains(fieldSchema.getType()));
                    }
                    case META_FIELD -> {
                        throw new UnsupportedOperationException("Meta fields are unsupported in between operation");
                    }
                    case FUNCTION -> {
                        throw new UnsupportedOperationException("Functions are unsupported in between operation");
                    }
                }
                val ticketAttributeTypeSet = Stream.<Class<?>>ofNullable(KNOWN_TICKET_ATTRIBUTES.get(name))
                                                        .filter(Objects::nonNull)
                                                        .collect(Collectors.toSet());
                val startValue = ticketExpressionValue(expr.getBetweenExpressionStart(),
                                                       elementType,
                                                       schema.get(name),
                                                       ticketAttributeTypeSet);
                val endValue = ticketExpressionValue(expr.getBetweenExpressionEnd(),
                                                     elementType,
                                                     schema.get(name),
                                                     ticketAttributeTypeSet);
                val start = readDateNumericValue(fieldSchema, elementType, name, startValue);
                val end = readDateNumericValue(fieldSchema, elementType, name, endValue);
                switch (elementType) {
                    case TICKET_ATTRIBUTE -> {
                        val startDate = (Date) start;
                        val endDate = (Date) end;
                        val from =  startDate.before(endDate) ? startDate:  endDate;
                        val duration =  Duration.milliseconds(Math.abs(startDate.getTime() - endDate.getTime()));
                        tfs.add(switch(name) {
                            case TicketSkeleton.Fields.created -> new TicketsCreatedTimeWindow(duration, from);
                            case TicketSkeleton.Fields.updated -> new TicketsUpdatedTimeWindow(duration, from);
                            default ->
                                    throw new UnsupportedOperationException("Between operation unsupported for field " + name );
                        });
                    }
                    case TICKET_FIELD -> {
                            val sortedValue = sortComparable(start, end);
                            ffs.add(switch (fieldSchema.getType()) {
                                case NUMBER, DATE -> new TicketFieldBetween(name, sortedValue.getFirst(), sortedValue.getSecond());
                                default ->
                                        throw new UnsupportedOperationException("Between operation unsupported for field " + name + " of type " + lowerSnake(
                                                fieldSchema.getType().getDisplayName()));
                            });
                    }
                }
            }

            record ComparisonParsedOutput(
                    String name,
                    ElementType elementType,
                    Object value,
                    FieldSchema fieldSchema,
                    Class<?> ticketAttributeType
            ) {
            }

            @Override
            public void visit(EqualsTo expr) {
                val parsed = parseComparisonOperator(expr);
                if (parsed.elementType == ElementType.TICKET_ATTRIBUTE) {
                    tfs.add(switch (parsed.name) {
                        case TicketSkeleton.Fields.assignedToGroupId ->
                                new TicketAssignedToGroup(Set.of(Objects.toString(parsed.value)), false);
                        case TicketSkeleton.Fields.assignedToUserId -> new TicketAssignedToUser(
                                Objects.toString(parsed.value), false);
                        case TicketSkeleton.Fields.ticketStateId -> new TicketStateIn(
                                Set.of(Objects.toString(parsed.value)), false);
                        case TicketSkeleton.Fields.subjectId -> new TicketSubjectEquals(
                                Objects.toString(parsed.value));
                        case TicketSkeleton.Fields.priority ->
                                new TicketPriorityIn(Set.of(TicketPriority.valueOf(parsed.value.toString())), false);
                        default ->
                                throw new IllegalArgumentException("Unknown ticket attribute in where clause: " + parsed.name);
                    });
                }
                else {
                    ffs.add(new TicketFieldEquals(parsed.fieldSchema.getId(), parsed.value));
                }
            }

            @Override
            public void visit(NotEqualsTo expr) {
                val parsed = parseComparisonOperator(expr);

                if (parsed.elementType == ElementType.TICKET_ATTRIBUTE) {
                    tfs.add(switch (parsed.name) {
                        case TicketSkeleton.Fields.assignedToGroupId ->
                                new TicketAssignedToGroup(Set.of(Objects.toString(parsed.value)), true);
                        case TicketSkeleton.Fields.assignedToUserId ->
                                new TicketAssignedToUser(Objects.toString(parsed.value),
                                                         true);
                        case TicketSkeleton.Fields.ticketStateId ->
                                new TicketStateIn(Set.of(Objects.toString(parsed.value)),
                                                  true);
                        case TicketSkeleton.Fields.priority ->
                                new TicketPriorityIn(Set.of(TicketPriority.valueOf(parsed.value.toString())), true);
                        default ->
                                throw new IllegalArgumentException("Unknown ticket attribute in where clause: " + parsed.name);
                    });
                }
                else {
                    ffs.add(new TicketFieldNotEquals(schema.get(parsed.name).getId(), parsed.value));
                }
            }

            @Override
            public void visit(GreaterThan expr) {
                val parsed = parseComparisonOperator(expr);
                ensureTicketField(parsed.elementType);
                val value = readDateNumericValue(parsed.fieldSchema, parsed.elementType, parsed.name, parsed.value);
                ffs.add(new TicketFieldGreater(parsed.fieldSchema.getId(), value));
            }

            @Override
            public void visit(GreaterThanEquals expr) {
                val parsed = parseComparisonOperator(expr);
                ensureTicketField(parsed.elementType);
                val value = readDateNumericValue(parsed.fieldSchema, parsed.elementType, parsed.name, parsed.value);
                ffs.add(new TicketFieldGreaterEquals(parsed.fieldSchema.getId(), value));

            }

            @Override
            public void visit(InExpression expr) {
                val name = fieldName(expr.getLeftExpression());
                val elementType = elementType(name, schema).orElse(null);

                ensureTicketAttributeOrTicketField(elementType);
                assert null != elementType;
                val fieldSchema = schema.get(fieldName(name));
                val ticketAttributeTypeSet = Stream.<Class<?>>ofNullable(KNOWN_TICKET_ATTRIBUTES.get(name))
                                                        .filter(Objects::nonNull)
                                                        .collect(Collectors.toSet());
                val values = new ArrayList<>();
                expr.getRightExpression().accept(new ExpressionVisitorAdapter() {

                    @Override
                    public void visit(ExpressionList<?> expressionList) {
                        expressionList.forEach(item -> values.add(ticketExpressionValue(item,
                                                                                        elementType,
                                                                                        fieldSchema,
                                                                                        ticketAttributeTypeSet)));
                    }
                });
                Preconditions.checkArgument(!values.isEmpty(),
                                            "No values found for in expression for " + name);
                if (elementType == ElementType.TICKET_ATTRIBUTE) {
                    tfs.add(switch (name) {
                        case TicketSkeleton.Fields.assignedToGroupId -> new TicketAssignedToGroup(values.stream()
                                                                                                          .map(Objects::toString)
                                                                                                          .collect(
                                                                                                                  Collectors.toUnmodifiableSet()),
                                                                                                  false);
                        case TicketSkeleton.Fields.ticketStateId -> new TicketStateIn(values.stream()
                                                                                              .map(Objects::toString)
                                                                                              .collect(
                                                                                                      Collectors.toUnmodifiableSet()),
                                                                                      false);
                        case TicketSkeleton.Fields.priority -> new TicketPriorityIn(values.stream()
                                                                                            .map(value -> TicketPriority.valueOf(
                                                                                                    value.toString()))
                                                                                            .collect(Collectors.toUnmodifiableSet()),
                                                                                    false);
                        default -> throw new IllegalArgumentException(
                                "Unknown or unsupported ticket attribute in where clause. Expr:  " + expr);
                    });
                }
                else {
                    ffs.add(new TicketFieldIn(fieldSchema.getId(), values, false));
                }
            }

            @Override
            public void visit(IsNullExpression expr) {
                val name = fieldName(expr.getLeftExpression());
                val elementType = elementType(name, schema).orElse(null);
                ensureTicketAttributeOrTicketField(elementType);
                assert null != elementType;

                val fieldSchema = schema.get(fieldName(name));


                if (elementType == ElementType.TICKET_ATTRIBUTE) {
                    tfs.add(switch (name) {
                        case TicketSkeleton.Fields.assignedToGroupId -> new TicketUnAssignedToGroup();
                        case TicketSkeleton.Fields.assignedToUserId -> new TicketUnAssignedToUser();
                        default -> throw new IllegalArgumentException(
                                "Unknown or unsupported ticket attribute in where clause. Expr: " + expr);
                    });
                }
                else {
                    ffs.add(new TicketFieldIsEmpty(fieldSchema.getId(), false));
                }
            }

            @Override
            public void visit(IsBooleanExpression expr) {
                val name = fieldName(expr.getLeftExpression());
                val elementType = elementType(name, schema).orElse(null);
                ensureTicketField(elementType);
                assert null != elementType;
                val fieldSchema = schema.get(name);
                Preconditions.checkArgument(null != fieldSchema && fieldSchema.getType().equals(FieldType.BOOLEAN),
                                            "Only boolean ticket fields are allowed in is boolean expression. Expr: " + expr);

                ffs.add(new TicketFieldEquals(fieldSchema.getId(), expr.isTrue()));
                //TODO HANDLE IS NOT
            }

            @Override
            public void visit(MinorThan expr) {
                val parsed = parseComparisonOperator(expr);
                ensureTicketField(parsed.elementType);
                val value = readDateNumericValue(parsed.fieldSchema, parsed.elementType, parsed.name, parsed.value);
                ffs.add(new TicketFieldLesser(parsed.fieldSchema.getId(), value));
            }

            @Override
            public void visit(MinorThanEquals expr) {
                val parsed = parseComparisonOperator(expr);
                ensureTicketField(parsed.elementType);
                val value = readDateNumericValue(parsed.fieldSchema, parsed.elementType, parsed.name, parsed.value);
                ffs.add(new TicketFieldLesserEquals(parsed.fieldSchema.getId(), value));
            }

            private ComparisonParsedOutput parseComparisonOperator(final ComparisonOperator expr) {
                var name = fieldName(expr.getLeftExpression());
                Preconditions.checkArgument(!Strings.isNullOrEmpty(name),
                                            "ticket attribute or field name is mandatory for comparison operator in " +
                                                    "while clause. Expr: " + expr.getStringExpression());
                val elementType = elementType(name, schema).orElse(null);
                Preconditions.checkArgument(elementType != null && Set.of(ElementType.TICKET_ATTRIBUTE,
                                                                          ElementType.TICKET_FIELD)
                                                    .contains(elementType),
                                            "Name can only be ticket attribute or ticket field in comparison operator" +
                                                    ". Error exp: " + expr.getStringExpression());
                name = Strings.isNullOrEmpty(name) ? fieldName(expr.getRightExpression()) : name;

                val fieldSchema = schema.get(fieldName(name));
                val ticketAttrType = ticketAttributeType(name).orElse(null);
                val ticketAttrTypeSet = null != ticketAttrType
                                        ? Set.<Class<?>>of(ticketAttrType)
                                        : Set.<Class<?>>of();
                var value = ticketExpressionValue(expr.getRightExpression(),
                                                  elementType,
                                                  fieldSchema,
                                                  ticketAttrTypeSet);
                value = null == value
                        ? ticketExpressionValue(expr.getLeftExpression(), elementType, fieldSchema, ticketAttrTypeSet)
                        : value;
                Preconditions.checkNotNull(value,
                                           "Value is mandatory for comparison operator. Expr: " + expr.getStringExpression());
                return new ComparisonParsedOutput(name, elementType, value, fieldSchema, ticketAttrType);
            }

            private Comparable<?> readDateNumericValue(
                    FieldSchema fieldSchema,
                    ElementType elementType,
                    String name,
                    Object value) {
                return Objects.requireNonNull(switch (elementType) {
                    case TICKET_ATTRIBUTE -> {
                        if (KNOWN_TICKET_DATE_ATTRIBUTES.containsKey(name)) {
                            yield (Date) value;
                        }
                        else {
                            if (NUMERIC_JAVA_TYPES.contains(KNOWN_TICKET_ATTRIBUTES.get(name))) {
                                yield (double) value;
                            }
                        }
                        yield null;
                    }
                    case TICKET_FIELD -> switch (fieldSchema.getType()) {
                        case NUMBER -> (double) value;
                        case DATE -> (Date) value;
                        default -> null;

                    };
                    case META_FIELD, FUNCTION, EVENT_ATTRIBUTE -> null;
                }, "Could nor parse comparable value field " + name);
            }
        });

        return response;
    }

    private static void ensureTicketField(ElementType elementType) {
        Preconditions.checkArgument(elementType == ElementType.TICKET_FIELD,
                                    "Provide a valid ticket field name in the where clause");
    }

    private static void ensureTicketAttributeOrTicketField(ElementType elementType) {
        Preconditions.checkArgument(elementType == ElementType.TICKET_ATTRIBUTE
                                            || elementType == ElementType.TICKET_FIELD,
                                    "Provide a valid ticket attribute or ticket field name in the lhs of " +
                                            "where clause");
    }

    private static void ensureEventAttribute(ElementType elementType) {
        Preconditions.checkArgument(elementType == ElementType.EVENT_ATTRIBUTE,
                                    "Provide a valid event attribute or ticket field name in the lhs of " +
                                            "where clause");
    }

    @NonNull
    private static Optional<Class<?>> ticketAttributeType(String name) {
        return Optional.ofNullable(KNOWN_TICKET_ATTRIBUTES.get(name));
    }

    private enum ElementType {
        TICKET_ATTRIBUTE,
        TICKET_FIELD,
        EVENT_ATTRIBUTE,
        META_FIELD,
        FUNCTION
    }

    private static Optional<ElementType> elementType(final String name, Map<String, FieldSchema> schema) {

        if (KNOWN_TICKET_ATTRIBUTES.containsKey(name)) {
            return Optional.of(ElementType.TICKET_ATTRIBUTE);
        }
        if (schema.containsKey(name)) {
            return Optional.of(ElementType.TICKET_FIELD);
        }
        if (KNOWN_META_FIELDS.contains(name)) {
            return Optional.of(ElementType.META_FIELD);
        }
        if (KNOWN_FUNCTIONS.contains(name)) {
            return Optional.of(ElementType.FUNCTION);
        }
        if (KNOWN_EVENTS_ATTRIBUTES.containsKey(name)) {
            return Optional.of(ElementType.EVENT_ATTRIBUTE);
        }
        return Optional.empty();
    }

    private static String fieldName(final String name) {
        return name.substring(name.indexOf(".") + 1);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static  Pair<Comparable<?>,Comparable<?>> sortComparable(Comparable<?> a, Comparable<?> b) {
        return (((Comparable) a).compareTo(b) <= 0) ? new Pair<>(a,b) : new Pair<>(b,a);
    }

}
