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
import io.appform.conductor.model.schema.SchemaSummary;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Storage system for {@link io.appform.conductor.model.schema.Schema}
 */
public interface SchemaManager {
    Optional<SchemaSummary> createSchema(final String name, final String description);

    Optional<SchemaSummary> getSchema(final String schemaId);

    List<SchemaSummary> listSchema(final Predicate<SchemaSummary> filter);

    Optional<SchemaSummary> updateSchema(final String schemaId, final UnaryOperator<SchemaSummary> updater);

    boolean deleteSchema(final String schemaId);

    Optional<FieldSchema> createField(final String schemaId, final FieldSchema schema);

    Optional<FieldSchema> getField(final String schemaId, final String fieldId);

    Optional<FieldSchema> update(final String schemaId, final String fieldId,
                                 final UnaryOperator<FieldSchema> updater);

    boolean deleteField(final String schemaId, final String fieldId);

    List<FieldSchema> listFields(final String schemaId, Predicate<FieldSchema> filter);
}
