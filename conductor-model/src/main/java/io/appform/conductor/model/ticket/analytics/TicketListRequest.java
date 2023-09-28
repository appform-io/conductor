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

package io.appform.conductor.model.ticket.analytics;

import io.appform.conductor.model.ticket.filter.Filters;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.Max;
import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketListRequest extends TicketQueryOperation {
    List<String> ticketCoreFields;
    List<String> ticketDataFields;
    List<String> functions;
    String next;
    @Max(1024)
    int size;

    @Builder
    @Jacksonized
    public TicketListRequest(
            String queryId,
            Filters filters,
            ResponseEncoding responseEncoding,
            @Singular List<String> ticketCoreFields,
            @Singular List<String> ticketDataFields,
            @Singular List<String> functions,
            String next, int size) {
        super(OpCode.LIST, queryId, filters, responseEncoding);
        this.ticketCoreFields = ticketCoreFields;
        this.ticketDataFields = ticketDataFields;
        this.functions = functions;
        this.next = next;
        this.size = size;
    }

    @Override
    public <T> T accpet(TicketQueryOperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
