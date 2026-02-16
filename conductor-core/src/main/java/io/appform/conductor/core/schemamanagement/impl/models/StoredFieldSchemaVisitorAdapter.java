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

package io.appform.conductor.core.schemamanagement.impl.models;

/**
 *
 */
public abstract class StoredFieldSchemaVisitorAdapter<T> implements StoredFieldSchemaVisitor<T> {
    private final T defaultValue;

    public StoredFieldSchemaVisitorAdapter() {
        this(null);
    }

    public StoredFieldSchemaVisitorAdapter(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public T visit(StoredStringFieldSchema stringField) {
        return defaultValue;
    }

    @Override
    public T visit(StoredBooleanFieldSchema booleanField) {
        return defaultValue;
    }

    @Override
    public T visit(StoredLocationFieldSchema locationField) {
        return defaultValue;
    }

    @Override
    public T visit(StoredDateFieldSchema dateField) {
        return defaultValue;
    }

    @Override
    public T visit(StoredNumberFieldSchema numberField) {
        return defaultValue;
    }

    @Override
    public T visit(StoredChoiceFieldSchema choiceField) {
        return defaultValue;
    }
}
