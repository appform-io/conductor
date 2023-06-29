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

package io.appform.conductor.model.ticket.fields;

import io.appform.conductor.model.schema.FieldType;
import lombok.Data;

/**
 * Representation for a value in the field
 */
@Data
public abstract class FieldValue {

    /**
     * Type of field
     */
    private final FieldType type;

    /**
     * Accept an implementation of {@link FieldValueVisitor} to perform subtype specific options
     * @param visitor The actual implementation
     * @param <R> Return type of the visitor
     * @return The actual result of processing
     */
    public abstract <R> R accept(final FieldValueVisitor<R> visitor);
}
