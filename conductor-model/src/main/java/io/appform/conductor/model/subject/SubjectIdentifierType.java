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

package io.appform.conductor.model.subject;

import lombok.Getter;

/**
 * Identifier for a subject
 */
public enum SubjectIdentifierType {

    /**
     * Phone number for subject
     */
    PHONE("Phone") {
        @Override
        public <T> T accept(SubjectIdentifierTypeVisitor<T> visitor) {
            return visitor.visitPhone();
        }
    },

    /**
     * Email for subject
     */
    EMAIL("Email") {
        @Override
        public <T> T accept(SubjectIdentifierTypeVisitor<T> visitor) {
            return visitor.visitEmail();
        }
    },

    /**
     * Govt identification
     */
    GOVT_ID("Government ID") {
        @Override
        public <T> T accept(SubjectIdentifierTypeVisitor<T> visitor) {
            return visitor.visitGovtID();
        }
    },

    /**
     * Govt identification
     */
    EXTERNAL_ID("Some other external ID") {
        @Override
        public <T> T accept(SubjectIdentifierTypeVisitor<T> visitor) {
            return visitor.visitExternalID();
        }
    }

    ;

    /**
     * Display name of the attribute
     */
    @Getter
    private final String displayName;

    SubjectIdentifierType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Accept an implementation of {@link SubjectIdentifierTypeVisitor} to handle eelemt specific operations
     * @param visitor Actual implementation of the visitor
     * @param <T> Return type of the implementation
     * @return Actual result of the element specific operations as implemented in the visitor
     */
    public abstract <T> T accept(final SubjectIdentifierTypeVisitor<T> visitor);

    /**
     * A visitor that can be implemented to code enum element specific operations
     * @param <T> Return type of the operation
     */
    public interface SubjectIdentifierTypeVisitor<T> {

        /**
         * Perform operations specific to {@link SubjectIdentifierType#PHONE}
         * @return Result of operation
         */
        T visitPhone();

        /**
         * Perform operations specific to {@link SubjectIdentifierType#EMAIL}
         * @return Result of operation
         */
        T visitEmail();

        /**
         * Perform operations specific to {@link SubjectIdentifierType#GOVT_ID}
         * @return Result of operation
         */
        T visitGovtID();


        /**
         * Perform operations specific to {@link SubjectIdentifierType#EXTERNAL_ID}
         * @return Result of operation
         */
        T visitExternalID();
    }
}
