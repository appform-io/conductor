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

import lombok.Getter;

/**
 *
 */
@Getter
public enum TicketFieldFilterType {
    EQUALS("Equals"),
    NOT_EQUALS("Not Equals"),
    GREATER("Greater Than"),
    GREATER_EQUALS("Greater Than or Equals"),
    LESSER("Lesser Than"),
    LESSER_EQUALS("Lesser Than or Equals"),
    BETWEEN("Between"),
    CONTAINS_CHOICES("Contains choices"),
    IN("In");

    private final String displayName;

    TicketFieldFilterType(String displayName) {
        this.displayName = displayName;
    }
}
