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

package io.appform.conductor.model.ticket.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.ticket.filter.fieldfilters.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "EQUALS", value = TicketFieldEquals.class),
    @JsonSubTypes.Type(name = "NOT_EQUALS", value = TicketFieldNotEquals.class),
    @JsonSubTypes.Type(name = "GREATER", value = TicketFieldGreater.class),
    @JsonSubTypes.Type(name = "GREATER_EQUALS", value = TicketFieldGreaterEquals.class),
    @JsonSubTypes.Type(name = "LESSER", value = TicketFieldLesser.class),
    @JsonSubTypes.Type(name = "LESSER_EQUALS", value = TicketFieldLesserEquals.class),
    @JsonSubTypes.Type(name = "BETWEEN", value = TicketFieldBetween.class),
    @JsonSubTypes.Type(name = "CONTAINS_CHOICES", value = TicketFieldContainsChoices.class),
    @JsonSubTypes.Type(name = "IS_EMPTY", value = TicketFieldIsEmpty.class),
})
@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TicketFieldFilter {
    private final TicketFieldFilterType type;
    private final String fieldSchemaId;

    public abstract <T> T accept(final TicketFieldFilterVisitor<T> visitor);
}
