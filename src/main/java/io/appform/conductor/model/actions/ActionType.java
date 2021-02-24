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

package io.appform.conductor.model.actions;


import lombok.Getter;

/**
 * Types of actions that can be performed. The action itself can be part of a transition, or part of a ticket.
 */
public enum ActionType {
    COMPOSITION("Composition of other actions") {
        @Override
        public <T> T accept(ActionTypeVisitor<T> visitor) {
            return visitor.visitComposition();
        }
    },
    WEBHOOK("Webhook") {
        @Override
        public <T> T accept(ActionTypeVisitor<T> visitor) {
            return visitor.visitWebhook();
        }
    },
    //ROUTE_TO_USER,
    ROUTE_TO_GROUP("Route ticket to a group") {
        @Override
        public <T> T accept(ActionTypeVisitor<T> visitor) {
            return visitor.visitRouteToGroup();
        }
    },
    ADD_COMMENT("Add a comment to the ticket") {
        @Override
        public <T> T accept(ActionTypeVisitor<T> visitor) {
            return visitor.visitAddComment();
        }
    },
    ADD_TICKET_ACTION("Add an action to the ticket") {
        @Override
        public <T> T accept(ActionTypeVisitor<T> visitor) {
            return visitor.visitAddTicketAction();
        }
    },
    CHANGE_PRIORITY("Change the priority of the ticket") {
        @Override
        public <T> T accept(ActionTypeVisitor<T> visitor) {
            return visitor.visitChangePriority();
        }
    },
    SET_FIELD("Set a value to a field in the ticket") {
        @Override
        public <T> T accept(ActionTypeVisitor<T> visitor) {
            return visitor.visitSetField();
        }
    },

    ;

    @Getter
    private final String displayName;

    ActionType(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Accept an implementation of {@link ActionTypeVisitor} and invoke corresponding function.
     * This can be used to operate on different types of the num in a compile-time safe manner.
     * @param visitor The actual implementation of the visitor
     * @param <T> Data type for the results of operation
     * @return The results of operation
     */
    public abstract <T> T accept(ActionTypeVisitor<T> visitor);

    /**
     * Can be implemented to operate based on different type of the enum.
     * @param <T> Return type of operation
     */
    public interface ActionTypeVisitor<T> {

        /**
         * Perform an operation specific to {@link ActionType#COMPOSITION}
         * @return Results of the operation
         */
        T visitComposition();

        /**
         * Perform an operation specific to {@link ActionType#WEBHOOK}
         * @return Results of the operation
         */
        T visitWebhook();

        /**
         * Perform an operation specific to {@link ActionType#ROUTE_TO_GROUP}
         * @return Results of the operation
         */
        T visitRouteToGroup();

        /**
         * Perform an operation specific to {@link ActionType#ADD_COMMENT}
         * @return Results of the operation
         */
        T visitAddComment();

        /**
         * Perform an operation specific to {@link ActionType#ADD_TICKET_ACTION}
         * @return Results of the operation
         */
        T visitAddTicketAction();


        /**
         * Perform an operation specific to {@link ActionType#CHANGE_PRIORITY}
         * @return Results of the operation
         */
        T visitChangePriority();

        /**
         * Perform an operation specific to {@link ActionType#SET_FIELD}
         * @return Results of the operation
         */
        T visitSetField();

    }
}
