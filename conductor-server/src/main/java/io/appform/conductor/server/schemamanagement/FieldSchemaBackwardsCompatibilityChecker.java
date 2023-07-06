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
import lombok.val;
import org.apache.commons.lang3.ClassUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 *
 */
@Singleton
public class FieldSchemaBackwardsCompatibilityChecker {
    public enum ValidationStatus {
        SUCCESS,
        FAILURE
    }

    private final SchemaStore store;

    @Inject
    public FieldSchemaBackwardsCompatibilityChecker(SchemaStore store) {
        this.store = store;
    }

    //TODO::implement type specific checks here
    public SchemaOpValidationResult<Void> validate(final String schemaId, final FieldSchema updated) {
        val existing = store.getField(schemaId, updated.getId()).orElse(null);
        if (null == existing) {
            return SchemaOpValidationResult.failure("No field found for given id");
        }
        return updated.accept(new FieldSchemaVisitor<>() {
            @Override
            public SchemaOpValidationResult<Void> visit(StringFieldSchema stringField) {
                return cast(existing, StringFieldSchema.class)
                        .map(d -> SchemaOpValidationResult.<Void>success())
                        .orElse(SchemaOpValidationResult.failure(typeCastErrorMessage(existing, stringField)));
            }

            @Override
            public SchemaOpValidationResult<Void> visit(NumberFieldSchema numberField) {
                return cast(existing, NumberFieldSchema.class)
                        .map(d -> SchemaOpValidationResult.<Void>success())
                        .orElse(SchemaOpValidationResult.failure(typeCastErrorMessage(existing, numberField)));
            }

            @Override
            public SchemaOpValidationResult<Void> visit(BooleanFieldSchema booleanField) {
                return cast(existing, BooleanFieldSchema.class)
                        .map(d -> SchemaOpValidationResult.<Void>success())
                        .orElse(SchemaOpValidationResult.failure(typeCastErrorMessage(existing, booleanField)));
            }

            @Override
            public SchemaOpValidationResult<Void> visit(LocationFieldSchema locationField) {
                return cast(existing, LocationFieldSchema.class)
                        .map(d -> SchemaOpValidationResult.<Void>success())
                        .orElse(SchemaOpValidationResult.failure(typeCastErrorMessage(existing, locationField)));
            }

            @Override
            public SchemaOpValidationResult<Void> visit(DateFieldSchema dateField) {
                return cast(existing, DateFieldSchema.class)
                        .map(d -> SchemaOpValidationResult.<Void>success())
                        .orElse(SchemaOpValidationResult.failure(typeCastErrorMessage(existing, dateField)));
            }

            @Override
            public SchemaOpValidationResult<Void> visit(ChoiceFieldSchema choiceField) {
                return cast(existing, ChoiceFieldSchema.class)
                        .map(d -> SchemaOpValidationResult.<Void>success())
                        .orElse(SchemaOpValidationResult.failure(typeCastErrorMessage(existing, choiceField)));
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
