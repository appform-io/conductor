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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.analytics.EventQueryResponseVisitor;
import io.appform.conductor.model.events.analytics.impl.EventGroupResponse;
import io.appform.conductor.model.events.analytics.impl.EventListResponse;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.eventmanagement.EventStore;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.validator.constraints.Length;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

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
                    });
        }
        catch (Exception e) {
            return new ConductorApiResponse<>(ConductorErrorCode.CQL_PARSING_ERROR,
                                              null,
                                              ConductorException.generateErrorMessage(ConductorErrorCode.CQL_PARSING_ERROR,
                                                                                      Map.of("cqlError",
                                                                                             e.getMessage()),
                                                                                      null));
        }
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
    }    private static TabularResponse tabulateEventsResponse(
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

