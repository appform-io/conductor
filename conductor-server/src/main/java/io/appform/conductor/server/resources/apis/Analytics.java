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

package io.appform.conductor.server.resources.apis;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.analytics.EventQueryResponseVisitor;
import io.appform.conductor.model.events.analytics.impl.EventGroupResponse;
import io.appform.conductor.model.events.analytics.impl.EventListResponse;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.dashboards.model.WidgetQueryResponse;
import io.appform.conductor.server.eventmanagement.EventStore;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.dropwizard.auth.Auth;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.jsqlparser.JSQLParserException;
import org.hibernate.validator.constraints.Length;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static io.appform.conductor.server.utils.ConductorServerUtils.dateFormatsForTimeResolution;

/**
 *
 */
@Path("/analytics/v1")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class Analytics {

    private final TicketManager ticketManager;
    private final EventStore eventStore;
    private final CQLEngine cqlEngine;

    @GET
    @Path("/group")
    public ConductorApiResponse<TicketGroupResponse> groupCount(
            @Auth ConductorUser user,
            @QueryParam("workflowId") @Length(max = 45) String workflowId,
            @QueryParam("priority") final TicketPriority priority,
            @QueryParam("stateIds") @Length(max = 45) final String stateId,
            @QueryParam("subjectId") @Length(max = 45) String subjectId,
            @QueryParam("groupId") @Length(max = 45) String groupId,
            @QueryParam("createdById") @Length(max = 45) String createdById,
            @QueryParam("assignedToId") @Length(max = 45) String assignedToId,
            @QueryParam("field") @NotEmpty String ticketPropertyName) {
        val ticketFilters = translateToTicketFilters(workflowId,
                                                     priority,
                                                     stateId,
                                                     subjectId,
                                                     groupId,
                                                     createdById,
                                                     assignedToId);
        return ConductorApiResponse.success(ticketManager.groupCount(ticketFilters, List.of(), ticketPropertyName));
    }

    @GET
    @Path("/query")
    public ConductorApiResponse<Object> queryTickets(
            @Auth ConductorUser user,
            @HeaderParam("X-Request-Id") @Length(max = 128) final String requestId,
            @QueryParam("query") final String query,
            @QueryParam("next") @Length(max = 1024) String next,
            @QueryParam("length") @DefaultValue("100") @Min(5) @Max(200) int size,
            @QueryParam("format") @DefaultValue("DEFAULT") final ResponseFormat responseFormat) {
        try {
            val parserOutput = cqlEngine.parse(query).orElse(null);
            if (null == parserOutput) {
                return ConductorApiResponse.success(null);
            }
            val queryResponse = CQLEngine.runQuery(requestId, next, size, parserOutput, ticketManager, eventStore);
            return ConductorApiResponse.success(
                    switch (responseFormat) {
                        case DEFAULT -> queryResponse;
                        case TABLE -> switch (queryResponse.getDomain()) {
                            case TICKETS -> tabulateTicketResponse(query,
                                                                   queryResponse,
                                                                   (CQLEngine.CQLTicketParserOutput) parserOutput);
                            case EVENTS -> tabulateEventsResponse(query,
                                                                  queryResponse,
                                                                  parserOutput);
                        };
                        case WIDGET -> widgetResponse(query,
                                                      queryResponse,
                                                      parserOutput);
                    });
        }
        catch (Exception e) {
            var cause = (Throwable) e;
            while (cause != null) {
                if (cause instanceof JSQLParserException pe) {
                    return ConductorApiResponse.failure(
                            ConductorErrorCode.CQL_PARSING_ERROR, pe.getMessage());
                }
                cause = e.getCause();
            }
            throw e;
        }
    }

    private Object widgetResponse(
            String query,
            CQLEngine.CQLQueryExecutionOutput queryResponse,
            CQLEngine.CQLParserOutput parserOutput) {
        val type = new AtomicReference<WidgetQueryResponse.Type>();
//        val datasets = new ArrayList<WidgetQueryResponse.DataSetElement>();
        val tableRef = new AtomicReference<Table<Integer, String, Object>>();
        switch (queryResponse.getDomain()) {
            case TICKETS -> {
                tableRef.set(ConductorServerUtils.tabulateTicketQueryResponse(queryResponse.getTicketQueryResponse(),
                                                                              List.of()));
            }
            case EVENTS -> {
                tableRef.set(ConductorServerUtils.tabulateEventQueryResponse(queryResponse.getEventQueryResponse()));
            }
        }
        if (null == tableRef.get()) {
            return null;
        }
        val table = tableRef.get();
        //Find which type of widget is needed
        val widgetType = switch (queryResponse.getDomain()) {
            case TICKETS -> queryResponse.getTicketQueryResponse()
                    .accept(new TicketQueryResponseVisitor<WidgetQueryResponse.Type>() {
                        @Override
                        public WidgetQueryResponse.Type visit(TicketListResponse listResponse) {
                            throw new IllegalArgumentException("List query is not supported in widget query");
                        }

                        @Override
                        public WidgetQueryResponse.Type visit(TicketGroupResponse groupResponse) {
                            return parserOutput.getGroupingElements()
                                           .stream()
                                           .noneMatch(groupingElement -> groupingElement.getType()
                                                   .equals(GroupingElement.Type.TIME_BUCKET))
                                   ? WidgetQueryResponse.Type.BAR
                                   : WidgetQueryResponse.Type.TIME_SERIES;
                        }
                    });
            case EVENTS -> queryResponse.getEventQueryResponse()
                    .accept(new EventQueryResponseVisitor<WidgetQueryResponse.Type>() {
                        @Override
                        public WidgetQueryResponse.Type visit(EventListResponse listResponse) {
                            throw new IllegalArgumentException("List query is not supported in widget query");
                        }

                        @Override
                        public WidgetQueryResponse.Type visit(EventGroupResponse groupResponse) {
                            return parserOutput.getGroupingElements()
                                           .stream()
                                           .noneMatch(groupingElement -> groupingElement.getType()
                                                   .equals(GroupingElement.Type.TIME_BUCKET))
                                   ? WidgetQueryResponse.Type.BAR
                                   : WidgetQueryResponse.Type.TIME_SERIES;
                        }
                    });
        };
        return switch (widgetType) {
            case BAR -> {
                val labels =
                        new ArrayList<>(ConductorServerUtils.aliasesForGroupingElements(parserOutput.getGroupingElements()));
                //Actual col names in table
                val cols = parserOutput.getGroupingElements()
                        .stream()
                        .filter(groupingElement -> groupingElement.getType().equals(GroupingElement.Type.COLUMN))
                        .map(ColumnGroupingElement.class::cast)
                        .map(ColumnGroupingElement::getAttribute)
                        .toList();
                val numCols = cols.size();
                yield switch (numCols) {
                    case 1 -> {
                        val col = cols.get(0);
                        val xValues = new ArrayList<String>();
                        val yValues = new ArrayList<>();
                        table.rowMap()
                                .forEach((row, columns) -> {
                                    xValues.add((String) columns.get(col));
                                    yValues.add(columns.get("count"));
                                });
                        yield new WidgetQueryResponse(WidgetQueryResponse.Type.BAR,
                                                      xValues,
                                                      List.of(new WidgetQueryResponse.DataSetElement(labels.get(0),
                                                                                                     yValues)));
                    }
                    case 2 -> {
                        val col = cols.get(0);
                        val stackCol = cols.get(1);
                        val stackXValues = new TreeMap<String, TreeMap<String, Long>>();
                        val stackYValues = new TreeSet<String>();

                        table.rowMap()
                                .forEach((row, columns) -> {
                                    val colValue = Objects.toString(columns.get(col));
                                    val stackColValue = Objects.toString(columns.get(stackCol));
                                    stackXValues.computeIfAbsent(colValue, key -> new TreeMap<>())
                                            .compute(stackColValue,
                                                     (key, existing) ->
                                                             Objects.requireNonNullElse(existing, 0L)
                                                                     + (long) columns.get("count"));

                                    stackYValues.add(stackColValue);
                                });
                        val datasets = new ArrayList<WidgetQueryResponse.DataSetElement>();
                        for (val stackValue : stackYValues) {
                            val data = new ArrayList<>();
                            for (val colValue : stackXValues.keySet()) {
                                data.add(stackXValues.getOrDefault(colValue, new TreeMap<>())
                                                 .getOrDefault(stackValue, 0L));
                            }
                            datasets.add(new WidgetQueryResponse.DataSetElement(stackValue, data));
                        }
                        yield new WidgetQueryResponse(
                                WidgetQueryResponse.Type.BAR,
                                stackXValues.keySet(),
                                datasets);
                    }
                    default -> throw new IllegalArgumentException("You can group by at most 2 fields for charts");
                };

            }
            case TIME_SERIES -> {
                val labels =
                        new ArrayList<>(ConductorServerUtils.aliasesForGroupingElements(parserOutput.getGroupingElements()));
                //Actual col names in table
                val cols = colListForGroupingElements(parserOutput);
                val numCols = cols.size();
                val dateFormats = dateFormatsForTimeResolution();
                val dateFormat = parserOutput.getGroupingElements()
                        .stream()
                        .filter(groupingElement -> groupingElement.getType().equals(GroupingElement.Type.TIME_BUCKET))
                        .map(TimeBucketGroupingElement.class::cast)
                        .map(timeBucketGroupingElement -> dateFormats.get(timeBucketGroupingElement.getResolution()))
                        .findFirst()
                        .orElse(null);
                Preconditions.checkNotNull(dateFormat, "Could not get date format for time bucket");
                yield switch (numCols) {
                    case 1 -> {
                        val col = cols.get(0);
                        val xValues = new ArrayList<String>();
                        val yValues = new ArrayList<>();
                        table.rowMap()
                                .forEach((row, columns) -> {
                                    xValues.add(dateStrToEpochStr(dateFormat, columns.get(col)));
                                    yValues.add(columns.get("count"));
                                });
                        yield new WidgetQueryResponse(WidgetQueryResponse.Type.TIME_SERIES,
                                                      xValues,
                                                      List.of(new WidgetQueryResponse.DataSetElement(labels.get(0),
                                                                                                     yValues)));
                    }
                    case 2 -> {
                        val col = cols.get(0);
                        val stackCol = cols.get(1);
                        val stackXValues = new TreeMap<String, TreeMap<String, Long>>();
                        val stackYValues = new TreeSet<String>();

                        table.rowMap()
                                .forEach((row, columns) -> {
                                    val colValue = Objects.toString(columns.get(col));
                                    val stackColValue = Objects.toString(columns.get(stackCol));
                                    stackXValues.computeIfAbsent(colValue, key -> new TreeMap<>())
                                            .compute(stackColValue,
                                                     (key, existing) ->
                                                             Objects.requireNonNullElse(existing, 0L)
                                                                     + (long) columns.get("count"));

                                    stackYValues.add(stackColValue);
                                });
                        val datasets = new ArrayList<WidgetQueryResponse.DataSetElement>();
                        for (val stackValue : stackYValues) {
                            val data = new ArrayList<>();
                            for (val colValue : stackXValues.keySet()) {
                                data.add(stackXValues.getOrDefault(colValue, new TreeMap<>())
                                                 .getOrDefault(stackValue, 0L));
                            }
                            datasets.add(new WidgetQueryResponse.DataSetElement(stackValue, data));
                        }
                        yield new WidgetQueryResponse(
                                WidgetQueryResponse.Type.TIME_SERIES,
                                stackXValues.keySet()
                                        .stream()
                                        .map(date -> dateStrToEpochStr(dateFormat, date))
                                        .toList(),
                                datasets);
                    }
                    default -> throw new IllegalArgumentException("You can group by at most 2 fields for charts");
                };

            }
            case PIE, FLARE -> null;
        };
/*
        val cols = table.columnKeySet();
        cols.forEach(col -> {
            labels.add(col);
            val rowsForCol = table.column(col);
            datasets.add(rowsForCol.keySet()
                    .stream()
                    .sorted()
                    .map(idx -> new WidgetQueryResponse.DataSetElement(col, rowsForCol.get(idx)))
                    .toList());
        });
*/
//        return new WidgetQueryResponse(WidgetQueryResponse.Type.BAR, labels, datasets);
    }

    @SneakyThrows
    private static String dateStrToEpochStr(
            SimpleDateFormat dateFormat,
            Object dateStr) {
        return Objects.toString(dateFormat.parse(Objects.toString(dateStr)).getTime());
    }

    @SneakyThrows
    private static String dateToEpochString(String date, SimpleDateFormat dateFormat)  {
        return Objects.toString(dateFormat.parse(date));
    }

    @NonNull
    private static List<String> colListForGroupingElements(CQLEngine.CQLParserOutput parserOutput) {
        return parserOutput.getGroupingElements()
                .stream()
                .map(groupingElement -> groupingElement.accept(new GroupingElementVisitor<String>() {
                    @Override
                    public String visit(ColumnGroupingElement columnGroupingElement) {
                        return columnGroupingElement.getAttribute();
                    }

                    @Override
                    public String visit(TimeBucketGroupingElement timeBucketGroupingElement) {
                        return timeBucketGroupingElement.getDateAttribute();
                    }
                }))
                .toList();
    }

    private static TabularResponse tabulateTicketResponse(
            String query,
            CQLEngine.CQLQueryExecutionOutput queryResponse,
            CQLEngine.CQLTicketParserOutput parserOutput) {
        val ticketQueryResponse = queryResponse.getTicketQueryResponse();
        val table = ConductorServerUtils.tabulateTicketQueryResponse(
                ticketQueryResponse, parserOutput.getSelectedFields());
        val metadata = new HashMap<String, Object>();
        metadata.put("domain", CQLEngine.QueryDomain.TICKETS);
        metadata.put("opCode", ticketQueryResponse.getOpCode());
        metadata.put("requestId", ticketQueryResponse.getRequestId());
        metadata.put("query", query);
        metadata.computeIfAbsent("colHeaders",
                                 key -> Lists.reverse(List.copyOf(table.columnKeySet())));
        metadata.putAll(ticketQueryResponse.accept(new TicketQueryResponseVisitor<Map<String, Object>>() {
            @Override
            public Map<String, Object> visit(TicketListResponse listResponse) {
                return ImmutableMap.<String, Object>builder()
                        .put("colHeaders",
                             ImmutableList.<String>builder()
                                     .add(TicketGist.Fields.ticketId)
                                     .add(TicketGist.Fields.title)
                                     .add(TicketGist.Fields.workflowName)
                                     .add(TicketGist.Fields.stateName)
                                     .add(TicketGist.Fields.terminated)
                                     .add(TicketGist.Fields.priority)
                                     .add(TicketGist.Fields.created)
                                     .add(TicketGist.Fields.updated)
                                     .addAll(parserOutput.getSelectedFields()
                                                     .stream()
                                                     .map(CQLEngine.SelectedField::name)
                                                     .map(name -> "fields_" + name)
                                                     .toList())
                                     .build()
                            )
                        .put("next", listResponse.getNext())
                        .build();
            }

            @Override
            public Map<String, Object> visit(TicketGroupResponse groupResponse) {
                return Map.of("colHeaders", ImmutableList.builder()
                        .addAll(ConductorServerUtils.aliasesForGroupingElements(parserOutput.getGroupingElements()))
                        .add("count")
                        .build());
            }

        }));

        return new TabularResponse(table, metadata);
    }

    private static TabularResponse tabulateEventsResponse(
            String query,
            CQLEngine.CQLQueryExecutionOutput queryResponse,
            CQLEngine.CQLParserOutput parserOutput) {
        val eventQueryResponse = queryResponse.getEventQueryResponse();
        val table = ConductorServerUtils.tabulateEventQueryResponse(
                eventQueryResponse);
        val metadata = new HashMap<String, Object>();
        metadata.put("domain", CQLEngine.QueryDomain.EVENTS);
        metadata.put("opCode", eventQueryResponse.getOpCode());
        //metadata.put("requestId", eventQueryResponse.getRequestId());
        metadata.put("query", query);
        metadata.computeIfAbsent("colHeaders",
                                 key -> Lists.reverse(List.copyOf(table.columnKeySet())));
        metadata.putAll(eventQueryResponse.accept(new EventQueryResponseVisitor<Map<String, Object>>() {
            @Override
            public Map<String, Object> visit(EventListResponse listResponse) {
                return ImmutableMap.<String, Object>builder()
                        .put("colHeaders",
                             ImmutableList.<String>builder()
                                     .add(Event.Fields.id)
                                     .add(Event.Fields.type)
                                     .add(Event.Fields.date)
                                     .add(Event.Fields.objectType)
                                     .add(Event.Fields.objectId)
                                     .add(Event.Fields.userId)
                                     .add("date." + Event.EventTime.Fields.year)
                                     .add("date." + Event.EventTime.Fields.month)
                                     .add("date." + Event.EventTime.Fields.day)
                                     .add("date." + Event.EventTime.Fields.hour)
                                     .add("date." + Event.EventTime.Fields.minute)
                                     .add("date." + Event.EventTime.Fields.second)
                                     .add("date." + Event.EventTime.Fields.millisecond)
                                     .build()
                            )
                        .put("next", listResponse.getNext())
                        .build();
            }

            @Override
            public Map<String, Object> visit(EventGroupResponse groupResponse) {
                return Map.of("colHeaders", ImmutableList.builder()
                        .addAll(ConductorServerUtils.aliasesForGroupingElements(parserOutput.getGroupingElements()))
                        .add("count")
                        .build());
            }

        }));

        return new TabularResponse(table, metadata);
    }

    public static List<TicketFilter> translateToTicketFilters(
            String workflowId,
            TicketPriority priority,
            String stateId,
            String subjectId,
            String groupId,
            String createdById,
            String assignedToId) {
        val ticketFilters = new ArrayList<TicketFilter>();
        if (!Strings.isNullOrEmpty(workflowId)) {
            ticketFilters.add(new TicketWorkflowEquals(workflowId));
        }
        if (null != priority) {
            ticketFilters.add(new TicketPriorityIn(Set.of(priority), false));
        }
        if (!Strings.isNullOrEmpty(stateId)) {
            ticketFilters.add(new TicketStateIn(Set.of(stateId), false));
        }
        if (!Strings.isNullOrEmpty(subjectId)) {
            ticketFilters.add(new TicketSubjectEquals(subjectId));
        }
        if (!Strings.isNullOrEmpty(groupId)) {
            ticketFilters.add(new TicketAssignedToGroup(Set.of(groupId), false));
        }
        if (!Strings.isNullOrEmpty(createdById)) {
            ticketFilters.add(new TicketCreatedBy(createdById));
        }
        if (!Strings.isNullOrEmpty(assignedToId)) {
            ticketFilters.add(new TicketAssignedToUser(assignedToId, false));
        }
        return ticketFilters;
    }
}

