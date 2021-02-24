/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.model.ticket;

import lombok.Getter;

/**
 * Priority for a ticket.
 */
public enum TicketPriority {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    @Getter
    private final String displayName;

    TicketPriority(String displayName) {
        this.displayName = displayName;
    }

    /**
     * A visitor to be implemented to peorform priority specific operations
     * @param <T> Return type for operation
     */
    public interface TicketPriorityVisitor<T> {

        /**
         * Perform {@link TicketPriority#LOW} priority specific operation
         * @param <T> Return type for operation
         * @return The actual result for operation
         */
        <T> T visitLow();

        /**
         * Perform {@link TicketPriority#MEDIUM} priority specific operation
         * @param <T> Return type for operation
         * @return The actual result for operation
         */
        <T> T visitMedium();

        /**
         * Perform {@link TicketPriority#HIGH} priority specific operation
         * @param <T> Return type for operation
         * @return The actual result for operation
         */
        <T> T visitHigh();

        /**
         * Perform {@link TicketPriority#URGENT} priority specific operation
         * @param <T> Return type for operation
         * @return The actual result for operation
         */
        <T> T visitUrgent();
    }
}
