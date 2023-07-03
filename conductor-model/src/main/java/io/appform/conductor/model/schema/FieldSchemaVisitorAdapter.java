/*
 * Copyright (c) 2023 Santanu Sinha
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
 *
 */
public class FieldSchemaVisitorAdapter<T> implements FieldSchemaVisitor<T> {
    private final T returnValue;

    public FieldSchemaVisitorAdapter(T returnValue) {
        this.returnValue = returnValue;
    }

    @Override
    public T visit(StringFieldSchema stringField) {
        return returnValue;
    }

    @Override
    public T visit(NumberFieldSchema numberField) {
        return returnValue;
    }

    @Override
    public T visit(BooleanFieldSchema booleanField) {
        return returnValue;
    }

    @Override
    public T visit(LocationFieldSchema locationField) {
        return returnValue;
    }

    @Override
    public T visit(DateFieldSchema dateField) {
        return returnValue;
    }

    @Override
    public T visit(ChoiceFieldSchema choiceField) {
        return returnValue;
    }
}
