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

import io.appform.conductor.model.ticket.fields.impl.*;

/**
 * This visitor can be implement to handle different {@link FieldValue} types for
 * performing type specific options like rendering
 */
public interface FieldValueVisitor<T> {

    T visit(final StringFieldValue stringFieldValue);

    T visit(final ChoiceFieldValue choiceFieldValue);

    T visit(final BooleanFieldValue booleanFieldValue);

    T visit(final NumberFieldValue numberFieldValue);

    T visit(final LocationFieldValue locationFieldValue);

    T visit(final DateFieldValue dateFieldValue);
}
