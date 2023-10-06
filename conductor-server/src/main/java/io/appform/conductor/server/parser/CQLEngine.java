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
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.model.ticket.filter.Filters;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.fieldfilters.*;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.ticketmanagement.TicketSkeleton;
import io.appform.conductor.server.utils.Pair;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    private static final Set<FieldType> COMPARABLE_TYPES = EnumSet.of(FieldType.DATE, FieldType.NUMBER);

    private static final Set<String> KNOWN_META_FIELDS = Set.of();
    private static final Set<String> KNOWN_FUNCTIONS = Set.of();

    private static final String TICKETS_DB_PREFIX = "tickets.";
    private static final String TICKETS_FIELDS_PREFIX = "fields.";

    private final WorkflowStore workflowStore;
    private final SchemaStore schemaStore;

    public record TimeSeriesDetails(TimeResolution resolution, String column) {
        public static final TimeSeriesDetails DEFAULT = new TimeSeriesDetails(TimeResolution.DAY,
                                                                              TicketSkeleton.Fields.updated);
    }

    public record CQLParserOutput(
            Filters filters,
            List<SelectedField> selectedFields,
            List<GroupingElement> groupingElements,
            TicketQueryOpCode opCode,
            TimeSeriesDetails timeSeriesDetails,
            long limit
    ) {
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public Optional<CQLParserOutput> parse(final String query) {
        if (Strings.isNullOrEmpty(query)) {
            return Optional.empty();
        }
        val stmt = CCJSqlParserUtil.parse(query);
        val select = toPlainSelect(stmt).orElse(null);
        Preconditions.checkNotNull(select, "Only select statements are supported");

        val workflowId = ticketWorkFlowId(select);
        val wf = workflowStore.read(workflowId).orElse(null);
        Preconditions.checkNotNull(wf, "Invalid workflow id");
        log.debug("Will proceed with workflow: {}", wf.getDisplayName());
        val schema = schemaStore.get(wf.getSchemaId()).orElse(null);
        Preconditions.checkNotNull(schema, "Invalid schema for workflow " + workflowId);
        val fieldSchema = Objects.requireNonNullElse(schema.getFields(), List.<FieldSchema>of())
                .stream()
                .collect(Collectors.toMap(FieldSchema::getName, java.util.function.Function.identity()));
        val selectedFields = selectedFields(select, fieldSchema);
        log.debug("Fields selected: {}", selectedFields);
        val filters = filter(workflowId, select, fieldSchema);
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
                                    Preconditions.checkArgument(KNOWN_TICKET_DATE_ATTRIBUTES.containsKey(dateCol),
                                                                "Only date columns are allowed here");
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
                            val elementType = elementType(colName, fieldSchema).orElse(null);
                            Preconditions.checkArgument(elementType == ElementType.TICKET_ATTRIBUTE
                                                                && KNOWN_TICKET_ATTRIBUTES.containsKey(colName),
                                                        "Only ticket attributes are allowed in group by clause. Expr:" +
                                                                " " + groupByExpr);
                            groupingElements.add(new ColumnGroupingElement(colName, colName));
                        }
                    }));
        }
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
        return Optional.of(new CQLParserOutput(filters,
                                               selectedFields.getSecond(),
                                               groupingElements,
                                               groupOpType.get(),
                                               timeSeriesDetails.get(),
                                               limit.get()));
    }

    private String ticketWorkFlowId(final PlainSelect select) {
        val wfId = new AtomicReference<String>();
        select.getFromItem().accept(new FromItemVisitorAdapter() {

            @Override
            public void visit(Table table) {
                val fqtn = table.getFullyQualifiedName();
                log.debug("Query table name = {}", fqtn);
                Preconditions.checkArgument(fqtn.startsWith(TICKETS_DB_PREFIX),
                                            "From should be 'tickets.workflowId'");
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
                    Preconditions.checkArgument(KNOWN_TICKET_ATTRIBUTES.containsKey(colName),
                                                "Unknown ticket attribute selected. Attribute: " + colName);
                    accessedColName.set(colName);
                }
                log.debug("Added selection field: {}", colName);
            }
        });
        return accessedColName.get();
    }


    private Object value(
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
                throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
            }

            @Override
            public void visit(DoubleValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Double.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.NUMBER)) {
                    response.set(value.getValue());
                }
                throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
            }

            @Override
            public void visit(LongValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Double.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.NUMBER)) {
                    response.set((double) value.getValue());
                }
                throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
            }


            @Override
            public void visit(TimeValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Date.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.DATE)) {
                    response.set(value.getValue());
                }
                throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
            }

            @Override
            public void visit(TimestampValue value) {
                if ((elementType == ElementType.TICKET_ATTRIBUTE && ticketAttrType.contains(Date.class))
                        || (elementType == ElementType.TICKET_FIELD && fieldSchema.getType() == FieldType.DATE)) {
                    response.set(value.getValue());
                }
                throw new IllegalArgumentException("Expected value of type " + fieldSchema.getType());
            }
        });
        Preconditions.checkNotNull(response.get(), "Please provide value in rhs of where clause");
        return response.get();
    }

    private Filters filter(
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
                        Preconditions.checkArgument(COMPARABLE_TYPES.contains(fieldSchema.getType()));
                    }
                    case META_FIELD -> {
                        throw new UnsupportedOperationException("Meta fields are unsupported in between operation");
                    }
                    case FUNCTION -> {
                        throw new UnsupportedOperationException("Functions are unsupported in between operation");
                    }
                }
                val startValue = value(expr.getBetweenExpressionStart(), elementType, schema.get(name), Set.of());
                val endValue = value(expr.getBetweenExpressionEnd(), elementType, schema.get(name), Set.of());
                val start = readDateNumericValue(fieldSchema, elementType, name, startValue);
                val end = readDateNumericValue(fieldSchema, elementType, name, endValue);
                ffs.add(switch (fieldSchema.getType()) {
                    case NUMBER, DATE -> new TicketFieldBetween(name, start, end);
                    default ->
                            throw new UnsupportedOperationException("Between operation unsupported for field " + name + " of type " + lowerSnake(
                                    fieldSchema.getType().getDisplayName()));
                });
            }

            record ComparisonParsedOutput(String name, ElementType elementType, Object value, FieldSchema fieldSchema,
                                          Class<?> ticketAttributeType) {
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
                val ticketAttributeTypeSet = KNOWN_TICKET_ATTRIBUTES.get(name) != null
                                             ? Set.<Class<?>>of(KNOWN_TICKET_ATTRIBUTES.get(name))
                                             : Set.<Class<?>>of();
                val values = new ArrayList<>();
                expr.getRightExpression().accept(new ExpressionVisitorAdapter() {

                    @Override
                    public void visit(ExpressionList<?> expressionList) {
                        expressionList.forEach(item -> values.add(value(item,
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
                var value = value(expr.getRightExpression(), elementType, fieldSchema, ticketAttrTypeSet);
                value = null == value
                        ? value(expr.getLeftExpression(), elementType, fieldSchema, ticketAttrTypeSet)
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
                        else if (Set.of(Long.class, Integer.class, Double.class).contains(KNOWN_TICKET_ATTRIBUTES.get(
                                name))) {
                            yield (double) value;
                        }
                        yield null;
                    }
                    case TICKET_FIELD -> switch (fieldSchema.getType()) {
                        case NUMBER -> (double) value;
                        case DATE -> (Date) value;
                        default -> null;

                    };
                    case META_FIELD, FUNCTION -> null;
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

    @NonNull
    private static Optional<Class<?>> ticketAttributeType(String name) {
        return Optional.ofNullable(KNOWN_TICKET_ATTRIBUTES.get(name));
    }

    private enum ElementType {
        TICKET_ATTRIBUTE,
        TICKET_FIELD,
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
        return Optional.empty();
    }

    private static String fieldName(final String name) {
        return name.substring(name.indexOf(".") + 1);
    }
}
