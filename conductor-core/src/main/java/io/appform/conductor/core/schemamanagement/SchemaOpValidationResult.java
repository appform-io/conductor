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

package io.appform.conductor.server.schemamanagement;

import lombok.Value;

import java.util.List;

/**
 *
 */
@Value
public class SchemaOpValidationResult<T> {
    FieldSchemaBackwardsCompatibilityChecker.ValidationStatus status;
    List<String> errors;
    T data;

    public static <T> SchemaOpValidationResult<T> success() {
        return new SchemaOpValidationResult<>(FieldSchemaBackwardsCompatibilityChecker.ValidationStatus.SUCCESS,
                                              List.of(),
                                              null);
    }

    public static <T> SchemaOpValidationResult<T> success(T data) {
        return new SchemaOpValidationResult<>(FieldSchemaBackwardsCompatibilityChecker.ValidationStatus.SUCCESS,
                                              List.of(),
                                              data);
    }

    public static <T> SchemaOpValidationResult<T> failure(final String error) {
        return failure(List.of(error));
    }

    public static <T> SchemaOpValidationResult<T> failure(final List<String> errors) {
        return new SchemaOpValidationResult<>(FieldSchemaBackwardsCompatibilityChecker.ValidationStatus.FAILURE, errors, null);
    }

}
