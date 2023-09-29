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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.model.ticket.filter.Filters;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.jsqlparser.JSQLParserException;
import org.hibernate.validator.constraints.Length;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.appform.conductor.server.resources.apis.Analytics.translateToTicketFilters;

/**
 *
 */
@Path("/tickets/v1")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class Tickets {

    private final TicketManager ticketManager;
    private final CQLEngine cqlEngine;

    @GET
    public ConductorApiResponse<TicketListResponse> searchTickets(
            @Auth ConductorUser user,
            @QueryParam("workflowId") @Length(max = 45) String workflowId,
            @QueryParam("priority") final TicketPriority priority,
            @QueryParam("stateIds") @Length(max = 45) final String stateId,
            @QueryParam("subjectId") @Length(max = 45) String subjectId,
            @QueryParam("groupId") @Length(max = 45) String groupId,
            @QueryParam("createdById") @Length(max = 45) String createdById,
            @QueryParam("assignedToId") @Length(max = 45) String assignedToId,
            @QueryParam("next") @Length(max = 1024) String next,
            @QueryParam("length") @DefaultValue("100") @Min(5) @Max(200) int size) {
        val ticketFilters = translateToTicketFilters(workflowId,
                                                     priority,
                                                     stateId,
                                                     subjectId,
                                                     groupId,
                                                     createdById,
                                                     assignedToId);
        return ConductorApiResponse.success(ticketManager.search(ticketFilters, List.of(), next, size));
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
            val results = cqlEngine.parse(query).orElse(null);
            if (null == results) {
                return ConductorApiResponse.success(null);
            }
            var ticketRequest = (TicketQueryRequest) null;
            val cols = new ArrayList<String>();
            if (null != results.groupingCols() && !results.groupingCols().isEmpty()) {
                ticketRequest = TicketGroupRequest.builder()
                        .queryId(requestId)
                        .filters(results.filters())
                        .groupingFields(results.groupingCols())
                        .build();
                cols.addAll(results.groupingCols());
            }
            else {
                ticketRequest = TicketListRequest.builder()
                        .queryId(requestId)
                        .filters(Objects.requireNonNullElse(results.filters(), Filters.EMPTY))
                        .direction(TicketListRequest.Direction.FORWARD)
                        .next(next)
                        .size(Math.min(200, Math.max(size, (int) results.limit())))
                        .build();
            }
            val queryResponse = ticketManager.query(ticketRequest);
            return ConductorApiResponse.success(
                    switch (responseFormat) {
                        case DEFAULT -> queryResponse;
                        case TABLE -> {
                            val table = tabulate(queryResponse);
                            val metadata = new HashMap<String, Object>();
                            metadata.put("opCode", queryResponse.getOpCode());
                            metadata.put("requestId", queryResponse.getRequestId());
                            metadata.putAll(queryResponse.accept(new TicketQueryResponseVisitor<Map<String, Object>>() {
                                @Override
                                public Map<String, Object> visit(TicketListResponse listResponse) {
                                    return Map.of("next", listResponse.getNext());
                                }

                                @Override
                                public Map<String, Object> visit(TicketGroupResponse groupResponse) {
                                    return Map.of("colHeaders", ImmutableList.builder()
                                            .addAll(results.groupingCols())
                                            .add("count")
                                            .build());
                                }

                                @Override
                                public Map<String, Object> visit(TicketTimeSeriesResponse timeSeriesResponse) {
                                    return Map.of();
                                }
                            }));
                            metadata.put("query", query);
                            metadata.computeIfAbsent("colHeaders", key -> Lists.reverse(List.copyOf(table.columnKeySet())));
                            yield new TabularResponse(table, metadata);
                        }
                    });
        }
        catch (Exception e) {
            log.error("Error: " + e.getMessage(), e);
            if (e instanceof JSQLParserException) {
                //TODO::throw
                return new ConductorApiResponse<>(ConductorErrorCode.CQL_PARSING_ERROR,
                                                        null,
                                                        ConductorException.generateErrorMessage(ConductorErrorCode.CQL_PARSING_ERROR,
                                                                                                Map.of("cqlError", e.getMessage()),
                                                                                                null));
            }
            throw e;
        }
    }

    private Table<Integer, String, Object> tabulate(TicketQueryResponse response) {
        val output = TreeBasedTable.<Integer, String, Object>create();
        val rowIdx = new AtomicInteger(0);
        response.accept(new TicketQueryResponseVisitor<Void>() {
            @Override
            public Void visit(TicketListResponse listResponse) {
                listResponse.getResults()
                        .forEach(gist -> {
                            val cols = output.row(rowIdx.incrementAndGet());
                            cols.put(TicketGist.Fields.ticketId, gist.getTicketId());
                            cols.put(TicketGist.Fields.title, gist.getTitle());
                            cols.put(TicketGist.Fields.workflowName, gist.getWorkflowName());
                            cols.put(TicketGist.Fields.stateName, gist.getStateName());
                            cols.put(TicketGist.Fields.terminated, gist.isTerminated());
                            cols.put(TicketGist.Fields.priority, gist.getPriority());
                            cols.put(TicketGist.Fields.created, gist.getCreated());
                            cols.put(TicketGist.Fields.updated, gist.getUpdated());
                        });
                return null;
            }

            @Override
            public Void visit(TicketGroupResponse groupResponse) {
                output.putAll(groupResponse.getCounts());
                return null;
            }

            @Override
            public Void visit(TicketTimeSeriesResponse timeSeriesResponse) {

                return null;
            }
        });
        return output;
    }
}

