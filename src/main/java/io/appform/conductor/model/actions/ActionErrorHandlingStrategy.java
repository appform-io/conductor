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
 * Defines the behaviour of action client, when action fails
 */
public enum ActionErrorHandlingStrategy {
    FAIL("Fail") {
        @Override
        public <T> T accept(ErrorHandlingStrategyVisitor<T> visitor) {
            return visitor.visitFail();
        }
    },
    IGNORE("Ignore") {
        @Override
        public <T> T accept(ErrorHandlingStrategyVisitor<T> visitor) {
            return visitor.visitFail();
        }
    },
    ;

    @Getter
    private final String displayName;

    ActionErrorHandlingStrategy(String displayName) {
        this.displayName = displayName;
    }

    public abstract <T> T accept(final ErrorHandlingStrategyVisitor<T> visitor);

    /**
     * Implement this for type specific handling for strategies
     *
     * @param <T>
     */
    public interface ErrorHandlingStrategyVisitor<T> {
        /**
         * Perform operation when error strategy is set to {@link ActionErrorHandlingStrategy#FAIL}
         *
         * @param <T> Type of operation result
         * @return Actual result of operation
         */
        <T> T visitFail();

        /**
         * Perform operation when error strategy is set to {@link ActionErrorHandlingStrategy#IGNORE}
         *
         * @param <T> Type of operation result
         * @return Actual result of operation
         */
        <T> T visitIgnore();
    }
}
