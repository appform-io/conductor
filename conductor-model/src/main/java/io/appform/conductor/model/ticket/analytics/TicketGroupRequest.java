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

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketGroupRequest extends TicketQueryRequest {

//    List<GroupFunction> functions;
    @NotEmpty
    List<String> groupingFields;

    @Builder
    @Jacksonized
    public TicketGroupRequest(
            String queryId,
            Filters filters,
//            List<GroupFunction> functions,
            List<String> groupingFields) {
        super(TicketQueryOpCode.GROUP, queryId, filters);
//        this.functions = functions;
        this.groupingFields = groupingFields;
    }

    @Override
    public <T> T accpet(TicketQueryOperationVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
