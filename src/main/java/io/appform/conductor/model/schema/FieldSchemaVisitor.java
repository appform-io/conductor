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

import io.appform.conductor.model.schema.fields.*;

/**
 * A visitor to handle different types of {@link FieldSchema}.
 * This can be implemented and used to validate inputs against the schema definition for example.
 * The type is parameterized and type-safe.
 */
public interface FieldSchemaVisitor<T> {
    T visit(final StringFieldSchema stringField);

    T visit(final NumberFieldSchema numberField);

    T visit(final BooleanFieldSchema booleanField);

    T visit(final LocationFieldSchema locationField);

    T visit(final DateFieldSchema dateField);

    T visit(final ChoiceFieldSchema choiceField);
}
