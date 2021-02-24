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

package io.appform.conductor.model.schema;

import lombok.Getter;

/**
 * Represents current state of a schema
 */
public enum SchemaState {

    /**
     * Schema is active and ticket creation is allowed
     */
    ACTIVE("Active") {
        @Override
        public <T> T accept(SchemaStateVisitor<T> visitor) {
            return visitor.visitActive();
        }
    },

    /**
     * Schema is inactive
     */
    INACTIVE("Inactive") {
        @Override
        public <T> T accept(SchemaStateVisitor<T> visitor) {
            return visitor.visitInactive();
        }
    };

    @Getter
    private final String displayName;

    SchemaState(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Accepts an implementation of {@link SchemaStateVisitor} fpr handling state specific operations
     * @param visitor The actual implementation fo the visitor
     * @param <T> Return type for implementation
     * @return Rerult of processing
     */
    public abstract <T> T accept(final SchemaStateVisitor<T> visitor);

    /**
     * Visitor can be implemented to handle validations and transitions etc.
     * @param <T> Return type of visitor
     */
    public interface SchemaStateVisitor<T> {

        T visitActive();

        T visitInactive();
    }
}
