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
 * Type for the field. This will govern the Field's behaviour, validations, rendering etc
 */
public enum FieldType {

    /**
     * String field type.
     */
    STRING("String") {
        @Override
        public <T> T accept(FieldTypeVisitor<T> visitor) {
            return visitor.visitString();
        }
    },
    /**
     * Field consists of a choice of fixed values
     */
    CHOICE("Choice") {
        @Override
        public <T> T accept(FieldTypeVisitor<T> visitor) {
            return visitor.visitChoice();
        }
    },
    /**
     * Boolean field type
     */
    BOOLEAN("Boolean") {
        @Override
        public <T> T accept(FieldTypeVisitor<T> visitor) {
            return visitor.visitBoolean();
        }
    },
    /**
     * Number field type. Will be represented as double
     */
    NUMBER("Number") {
        @Override
        public <T> T accept(FieldTypeVisitor<T> visitor) {
            return visitor.visitNumber();
        }
    },
    /**
     * Location field type. Will store latitude and longitude
     */
    LOCATION("Location") {
        @Override
        public <T> T accept(FieldTypeVisitor<T> visitor) {
            return visitor.visitLocation();
        }
    },
    /**
     * Date field type. Will store value as epoch.
     */
    DATE("Date") {
        @Override
        public <T> T accept(FieldTypeVisitor<T> visitor) {
            return visitor.visitDate();
        }
    };

    /**
     * Display name
     */
    @Getter
    private final String displayName;

    FieldType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Accept an implementation of {@link FieldTypeVisitor} that can be used to perform type specific options
     * @param visitor The actual implementation.
     * @param <T> Return type of the visitor
     * @return The result of processing in the visitor
     */
    public abstract <T> T accept(FieldTypeVisitor<T> visitor);

    /**
     * To be implemented to handle type specific validations, creation and operations etc
     * @param <T> Return type of the visitor
     */
    public interface FieldTypeVisitor<T> {

        /**
         * Needs to be overridden to handle a {@link FieldType#STRING} type field
         * @return Result of processing
         */
        T visitString();

        /**
         * Needs to be overridden to handle a {@link FieldType#CHOICE} type field
         * @return Result of processing
         */
        T visitChoice();

        /**
         * Needs to be overridden to handle a {@link FieldType#BOOLEAN} type field
         * @return Result of processing
         */
        T visitBoolean();

        /**
         * Needs to be overridden to handle a {@link FieldType#NUMBER} type field
         * @return Result of processing
         */
        T visitNumber();

        /**
         * Needs to be overridden to handle a {@link FieldType#LOCATION} type field
         * @return Result of processing
         */
        T visitLocation();

        /**
         * Needs to be overridden to handle a {@link FieldType#DATE} type field
         * @return Result of processing
         */
        T visitDate();
    }
}
