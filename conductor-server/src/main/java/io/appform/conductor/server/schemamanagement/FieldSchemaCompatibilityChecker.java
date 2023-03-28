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

import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldSchemaVisitor;
import io.appform.conductor.model.schema.fields.*;
import io.appform.conductor.server.schemamanagement.impl.SchemaStore;
import lombok.Value;
import lombok.val;
import org.apache.commons.lang3.ClassUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@Singleton
public class FieldSchemaCompatibilityChecker {
    public enum ValidationStatus {
        SUCCESS,
        FAILURE
    }

    @Value
    public static class ValidationResult {
        ValidationStatus status;
        List<String> errors;

        public static ValidationResult success() {
            return new ValidationResult(ValidationStatus.SUCCESS, List.of());
        }

        public static ValidationResult failure(final String error) {
            return failure(List.of(error));
        }

        public static ValidationResult failure(final List<String> errors) {
            return new ValidationResult(ValidationStatus.FAILURE, errors);
        }

    }

    private final SchemaStore store;

    @Inject
    public FieldSchemaCompatibilityChecker(SchemaStore store) {
        this.store = store;
    }

    public ValidationResult validate(final String schemaId, final FieldSchema updated) {
        val existing = store.getField(schemaId, updated.getId()).orElse(null);
        if (null == existing) {
            return ValidationResult.failure("No field found for given id");
        }
        return updated.accept(new FieldSchemaVisitor<>() {
            @Override
            public ValidationResult visit(StringFieldSchema stringField) {
                return cast(existing, StringFieldSchema.class)
                        .map(d -> ValidationResult.success())
                        .orElse(ValidationResult.failure(typeCastErrorMessage(existing, stringField)));
            }

            @Override
            public ValidationResult visit(NumberFieldSchema numberField) {
                return cast(existing, NumberFieldSchema.class)
                        .map(d -> ValidationResult.success())
                        .orElse(ValidationResult.failure(typeCastErrorMessage(existing, numberField)));
            }

            @Override
            public ValidationResult visit(BooleanFieldSchema booleanField) {
                return cast(existing, BooleanFieldSchema.class)
                        .map(d -> ValidationResult.success())
                        .orElse(ValidationResult.failure(typeCastErrorMessage(existing, booleanField)));
            }

            @Override
            public ValidationResult visit(LocationFieldSchema locationField) {
                return cast(existing, LocationFieldSchema.class)
                        .map(d -> ValidationResult.success())
                        .orElse(ValidationResult.failure(typeCastErrorMessage(existing, locationField)));
            }

            @Override
            public ValidationResult visit(DateFieldSchema dateField) {
                return cast(existing, DateFieldSchema.class)
                        .map(d -> ValidationResult.success())
                        .orElse(ValidationResult.failure(typeCastErrorMessage(existing, dateField)));
            }

            @Override
            public ValidationResult visit(ChoiceFieldSchema choiceField) {
                return cast(existing, ChoiceFieldSchema.class)
                        .map(d -> ValidationResult.success())
                        .orElse(ValidationResult.failure(typeCastErrorMessage(existing, choiceField)));
            }
        });
    }

    private static <T extends FieldSchema> String typeCastErrorMessage(FieldSchema existing, T stringField) {
        return "Field of type " + existing.getType() + " cannot be " +
                "converted to " + stringField.getClass()
                .getSimpleName();
    }

    private static <T extends FieldSchema> Optional<T> cast(final FieldSchema schema, final Class<T> clazz) {
        if (ClassUtils.isAssignable(schema.getClass(), clazz)) {
            return Optional.of((T) schema);
        }
        return Optional.empty();
    }
}
