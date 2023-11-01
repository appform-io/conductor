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

import com.google.common.collect.*;
import io.appform.conductor.model.apis.ConductorApiResponse;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.utils.ConductorServerUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            val queryResponse = CQLEngine.runQuery(requestId, next, size, results, ticketManager);
            return ConductorApiResponse.success(
                    switch (responseFormat) {
                        case DEFAULT -> queryResponse;
                        case TABLE -> {
                            val table = ConductorServerUtils.tabulate(queryResponse, results.selectedFields());
                            val metadata = new HashMap<String, Object>();
                            metadata.put("opCode", queryResponse.getOpCode());
                            metadata.put("requestId", queryResponse.getRequestId());
                            metadata.put("query", query);
                            metadata.computeIfAbsent("colHeaders",
                                                     key -> Lists.reverse(List.copyOf(table.columnKeySet())));
                            metadata.putAll(queryResponse.accept(new TicketQueryResponseVisitor<Map<String, Object>>() {
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
                                                         .addAll(results.selectedFields()
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
                                            .addAll(ConductorServerUtils.aliasesForGroupingElements(results.groupingElements()))
                                            .add("count")
                                            .build());
                                }

                            }));

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
                                                                                          Map.of("cqlError",
                                                                                                 e.getMessage()),
                                                                                          null));
            }
            throw e;
        }
    }

}

