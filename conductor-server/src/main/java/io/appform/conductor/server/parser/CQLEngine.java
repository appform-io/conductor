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
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.filter.Filters;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.fieldfilters.*;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.ticketmanagement.TicketSkeleton;
import io.appform.conductor.server.utils.Pair;
import io.appform.conductor.server.workflowmanagement.WorkflowStore;
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
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
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

    private static final String TICKETS_DB_PREFIX = "tickets.";
    private static final String TICKETS_FIELDS_PREFIX = "fields.";

    private final WorkflowStore workflowStore;
    private final SchemaStore schemaStore;
    private final TicketManager ticketManager;

    @SneakyThrows
    public Filters parse(String query) {
        if(Strings.isNullOrEmpty(query)) {
            return new Filters(List.of(), List.of());
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
        val selectedFields = selectedFields(select);
        log.debug("Fields selected: {}", selectedFields);
        return filter(workflowId, select, fieldSchema);
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

    private Pair<List<String>, List<String>> selectedFields(
            final PlainSelect plainSelect) {
        val ticketAttributes = new ArrayList<String>();
        val ticketFields = new ArrayList<String>();

        plainSelect.accept(new SelectVisitorAdapter() {

            @Override
            public void visit(PlainSelect plainSelect) {
                plainSelect.getSelectItems().forEach(item -> item.accept(new ExpressionVisitorAdapter() {
                    @Override
                    public void visit(AllColumns all) {
                        log.debug("All columns selected");
                    }

                    @Override
                    public void visit(Column column) {
                        val fieldName = fieldName(column);
                        if (fieldName.startsWith(TICKETS_FIELDS_PREFIX)) {
                            ticketFields.add(fieldName);
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

/*
    private String fieldName(SelectItem<?> selectItem, final Map<String, FieldSchema> schema) {
        return fieldName(selectItem.getExpression(), schema);
    }
*/

    private String fieldName(Expression expression) {
        val accessedColName = new AtomicReference<String>();
        expression.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(Column column) {
                val colName = column.getFullyQualifiedName();
                if (colName.startsWith("fields.")) {
                    val fieldName = fieldName(colName);
                    Preconditions.checkNotNull(fieldName,
                                               "Unknown ticket field name: " + fieldName);
                }
                else {
                    try {
                        TicketSkeleton.Fields.class.getField(colName);
                    }
                    catch (NoSuchFieldException e) {
                        log.error("Looking for non-existent ticket attribute: {}", colName);
                    }
                }
                accessedColName.set(colName);
                log.debug("Added selection field: {}", colName);
            }
        });
        Preconditions.checkNotNull(accessedColName.get(),
                                   "Please provide ticket attribute or field name on lhs of where clause");
        return accessedColName.get();
    }


    private Object value(Expression expression, boolean ticketField, FieldSchema fieldSchema) {
        val expectedType = ticketField ? FieldType.STRING : fieldSchema.getType();
        val response = new AtomicReference<>();
        expression.accept(new ExpressionVisitorAdapter() {

            @Override
            @SneakyThrows
            public void visit(StringValue value) {
                val convertedValue
                        = ticketField
                          ? value.getValue()
                          : (switch (expectedType) {

                              case STRING, CHOICE -> value.getValue();
                              case BOOLEAN -> Boolean.parseBoolean(value.getValue());
                              case NUMBER -> Double.parseDouble(value.getValue());
                              case DATE -> new SimpleDateFormat("yyyy-MM-dd").parse(value.getValue());
                              default -> null;
                          });
                response.set(convertedValue);
            }

            @Override
            public void visit(DateValue value) {
                ensureNotTicketField(ticketField);
                ensureValueTypeMatch(expectedType, Set.of(FieldType.DATE));
                response.set(value.getValue());
            }

            @Override
            public void visit(DoubleValue value) {
                ensureNotTicketField(ticketField);
                ensureValueTypeMatch(expectedType, Set.of(FieldType.NUMBER));
                response.set(value.getValue());
            }

            @Override
            public void visit(LongValue value) {
                ensureNotTicketField(ticketField);
                ensureValueTypeMatch(expectedType, Set.of(FieldType.NUMBER));
                response.set(value.getValue());
            }


            @Override
            public void visit(TimeValue value) {
                ensureNotTicketField(ticketField);
                ensureValueTypeMatch(expectedType, Set.of(FieldType.DATE));
                response.set(value.getValue());
            }

            @Override
            public void visit(TimestampValue value) {
                ensureNotTicketField(ticketField);
                ensureValueTypeMatch(expectedType, Set.of(FieldType.DATE));
                response.set(value.getValue());
            }

            //TODO::THE FOLLOWING FUNCTION NEEDS TO CHANGE TO MATCH TYPES FOR TicketSkeleton FIELDS
            private static void ensureNotTicketField(boolean ticketField) {
                Preconditions.checkArgument(!ticketField,
                                            "Mapping not possible for ticket field");
            }

            private static void ensureValueTypeMatch(FieldType expectedType, Set<FieldType> actualTypes) {
                Preconditions.checkArgument(actualTypes.contains(expectedType),
                                            "Type mismatch. Actual col type: " + expectedType + ". Provided: " + actualTypes);
            }
        });
        Preconditions.checkNotNull(response.get(),
                                   "Please provide value in rhs of where clause");
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
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(name);

                Preconditions.checkArgument(!ticketAttribute && null != fieldSchema, //TODO::TICKET DATE ATTRIBUTES
                                            "Provide a valid ticket field name in the lhs of " +
                                                    "where between clause");
                val lhs = readDateNumericValue(expr.getBetweenExpressionStart(), fieldSchema, ticketAttribute, name);
                val rhs = readDateNumericValue(expr.getBetweenExpressionStart(), fieldSchema, ticketAttribute, name);
                ffs.add(switch (fieldSchema.getType()) {
                    case NUMBER -> new TicketFieldBetween(name, (double) lhs, (double) rhs);
                    case DATE -> new TicketFieldDateBetween(name, (Date) lhs, (Date) rhs);
                    default ->
                            throw new UnsupportedOperationException("Between operation unsupported for field " + name + " of type " + lowerSnake(
                                    fieldSchema.getType().getDisplayName()));
                });
            }

            @Override
            public void visit(EqualsTo expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(ticketAttribute || null != fieldSchema,
                                            "Provide a valid ticket attribute or ticket field name in the lhs of " +
                                                    "where equals clause");
                val value = value(expr.getRightExpression(),
                                  ticketAttribute,
                                  fieldSchema);
                if (ticketAttribute) {
                    tfs.add(switch (name) {
                        case TicketSkeleton.Fields.assignedToGroupId ->
                                new TicketAssignedToGroup(Set.of(Objects.toString(value)), false);
                        case TicketSkeleton.Fields.assignedToUserId -> new TicketAssignedToUser(Objects.toString(value),
                                                                                                false);
                        case TicketSkeleton.Fields.ticketStateId -> new TicketStateIn(Set.of(Objects.toString(value)),
                                                                                      false);
                        case TicketSkeleton.Fields.subjectId -> new TicketSubjectEquals(Objects.toString(value));
                        case TicketSkeleton.Fields.priority ->
                                new TicketPriorityIn(Set.of(TicketPriority.valueOf(value.toString())), false);
                        default ->
                                throw new IllegalArgumentException("Unknown ticket attribute in where clause: " + name);
                    });
                }
                else {
                    ffs.add(new TicketFieldEquals(fieldSchema.getId(), value));
                }
            }

            @Override
            public void visit(NotEqualsTo expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(ticketAttribute || null != fieldSchema,
                                            "Provide a valid ticket attribute or ticket field name in the lhs of " +
                                                    "where not equals clause");
                val value = value(expr.getRightExpression(),
                                  ticketAttribute,
                                  fieldSchema);
                if (ticketAttribute) {
                    tfs.add(switch (name) {
                        case TicketSkeleton.Fields.assignedToGroupId ->
                                new TicketAssignedToGroup(Set.of(Objects.toString(value)), true);
                        case TicketSkeleton.Fields.assignedToUserId -> new TicketAssignedToUser(Objects.toString(value),
                                                                                                true);
                        case TicketSkeleton.Fields.ticketStateId -> new TicketStateIn(Set.of(Objects.toString(value)),
                                                                                      true);
                        case TicketSkeleton.Fields.priority ->
                                new TicketPriorityIn(Set.of(TicketPriority.valueOf(value.toString())), true);
                        default ->
                                throw new IllegalArgumentException("Unknown ticket attribute in where clause: " + name);
                    });
                }
                else {
                    ffs.add(new TicketFieldNotEquals(fieldSchema.getId(), value));
                }
            }

            @Override
            public void visit(GreaterThan expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(!ticketAttribute && null != fieldSchema, //TODO::TICKET DATE ATTRIBUTES
                                            "Provide a valid ticket field name in the lhs of " +
                                                    "where greater than clause");

                val value = readDateNumericValue(expr.getRightExpression(), fieldSchema, ticketAttribute, name);
                ffs.add(new TicketFieldGreater(fieldSchema.getId(), (double) value)); //TODO::DATE
            }

            @Override
            public void visit(GreaterThanEquals expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(!ticketAttribute && null != fieldSchema, //TODO::TICKET DATE ATTRIBUTES
                                            "Provide a valid ticket field name in the lhs of " +
                                                    "where greater than equals clause");

                val value = readDateNumericValue(expr.getRightExpression(), fieldSchema, ticketAttribute, name);
                ffs.add(new TicketFieldGreaterEquals(fieldSchema.getId(), (double) value)); //TODO::DATE
            }

            @Override
            public void visit(InExpression expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(ticketAttribute || null != fieldSchema,
                                            "Provide a valid ticket attribute or ticket field name in the lhs of " +
                                                    "where equals clause");
                val values = new ArrayList<>();
                expr.getRightExpression().accept(new ExpressionVisitorAdapter() {

                    @Override
                    public void visit(ExpressionList<?> expressionList) {
                        expressionList.forEach(item -> values.add(value(item, ticketAttribute, fieldSchema)));
                    }
                });
                Preconditions.checkArgument(!values.isEmpty(),
                                            "No values found for in expression for " + name);
                if (ticketAttribute) {
                    tfs.add(switch (name) {
                        case TicketSkeleton.Fields.assignedToGroupId -> new TicketAssignedToGroup(
                                values.stream()
                                        .map(Objects::toString)
                                        .collect(Collectors.toUnmodifiableSet()),
                                false);
                        case TicketSkeleton.Fields.ticketStateId -> new TicketStateIn(values.stream()
                                                                                              .map(Objects::toString)
                                                                                              .collect(
                                                                                                      Collectors.toUnmodifiableSet()),
                                                                                      false);
                        case TicketSkeleton.Fields.priority ->
                                new TicketPriorityIn(values.stream()
                                                             .map(value -> TicketPriority.valueOf(value.toString()))
                                                             .collect(Collectors.toUnmodifiableSet()), false);
                        default ->
                                throw new IllegalArgumentException("Unknown ticket attribute in where clause: " + name);
                    });
                }
                else {
                    ffs.add(new TicketFieldIn(fieldSchema.getId(), values, false));
                }
            }

            @Override
            public void visit(IsNullExpression expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(ticketAttribute || null != fieldSchema,
                                            "Provide a valid ticket attribute or ticket field name in the lhs of " +
                                                    "where equals clause");

                if (ticketAttribute) {
                    tfs.add(switch (name) {
                        case TicketSkeleton.Fields.assignedToGroupId -> new TicketUnAssignedToGroup();
                        case TicketSkeleton.Fields.assignedToUserId -> new TicketUnAssignedToUser();
                        default ->
                                throw new IllegalArgumentException("Unknown ticket attribute in where clause: " + name);
                    });
                }
                else {
                    ffs.add(new TicketFieldIsEmpty(fieldSchema.getId(), false));
                }
            }

            @Override
            public void visit(MinorThan expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(!ticketAttribute && null != fieldSchema, //TODO::TICKET DATE ATTRIBUTES
                                            "Provide a valid ticket field name in the lhs of " +
                                                    "where lesser than clause");

                val value = readDateNumericValue(expr.getRightExpression(), fieldSchema, ticketAttribute, name);
                ffs.add(new TicketFieldLesser(fieldSchema.getId(), (double) value)); //TODO::DATE
            }

            @Override
            public void visit(MinorThanEquals expr) {
                val name = fieldName(expr.getLeftExpression());
                val ticketAttribute = isTicketAttributeQuery(name);
                val fieldSchema = schema.get(fieldName(name));

                Preconditions.checkArgument(!ticketAttribute && null != fieldSchema, //TODO::TICKET DATE ATTRIBUTES
                                            "Provide a valid ticket field name in the lhs of " +
                                                    "where lesser than equals clause");

                val value = readDateNumericValue(expr.getRightExpression(), fieldSchema, ticketAttribute, name);
                ffs.add(new TicketFieldLesserEquals(fieldSchema.getId(), (double) value)); //TODO::DATE
            }

            private Serializable readDateNumericValue(
                    Expression expr,
                    FieldSchema fieldSchema,
                    boolean ticketAttribute,
                    String name) {
                return switch (fieldSchema.getType()) {
                    case NUMBER -> (double) value(expr, ticketAttribute, fieldSchema);
                    case DATE -> (Date) value(expr, ticketAttribute, fieldSchema);
                    default ->
                            throw new IllegalArgumentException("Between operation unsupported for field " + name + " " +
                                                                       "of type " + lowerSnake(
                                    fieldSchema.getType().getDisplayName()));
                };
            }

        });

        return response;
    }

    private static boolean isTicketAttributeQuery(final String name) {
        return !name.startsWith(TICKETS_FIELDS_PREFIX);
    }

    private static String fieldName(final String name) {
        return name.substring(name.indexOf(".") + 1);
    }
}
