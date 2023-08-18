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
 *
 */
public enum ActionExecutionResult {
    SUCCESS("Success") {
        @Override
        public <T> T accept(ActionExecutionResultVisitor<T> visitor) {
            return visitor.visitSuccess();
        }
    },
    FAILURE("Failure") {
        @Override
        public <T> T accept(ActionExecutionResultVisitor<T> visitor) {
            return visitor.visitSuccess();
        }
    },
    ;

    @Getter
    private final String displayName;

    ActionExecutionResult(String displayName) {
        this.displayName = displayName;
    }

    public abstract <T> T accept(final ActionExecutionResultVisitor<T> visitor);

    /**
     * Implement this for type specific handling for strategies
     *
     * @param <T>
     */
    public interface ActionExecutionResultVisitor<T> {
        /**
         * Perform operation when error strategy is set to {@link ActionExecutionResult#SUCCESS}
         *
         * @param <T> Type of operation result
         * @return Actual result of operation
         */
        <T> T visitSuccess();

        /**
         * Perform operation when error strategy is set to {@link ActionExecutionResult#FAILURE}
         *
         * @param <T> Type of operation result
         * @return Actual result of operation
         */
        <T> T visitFailure();
    }
}
