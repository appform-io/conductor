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

package io.appform.conductor.model.ticket.filter.fieldfilters;

import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFieldFilterType;
import io.appform.conductor.model.ticket.filter.TicketFieldFilterVisitor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketFieldNotEquals extends TicketFieldFilter {
    Object value;
    @Jacksonized
    @Builder
    public TicketFieldNotEquals(String fieldSchemaId, Object value) {
        super(TicketFieldFilterType.NOT_EQUALS, fieldSchemaId);
        this.value = value;
    }

    @Override
    public <T> T accept(TicketFieldFilterVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
