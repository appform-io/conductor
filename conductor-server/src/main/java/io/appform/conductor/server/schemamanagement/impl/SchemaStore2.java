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

package io.appform.conductor.server.schemamanagement.impl;

import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.model.schema.SchemaSummary;

import java.util.Optional;

/**
 *
 */
public interface SchemaStore2 {
    Optional<SchemaSummary> create(final String name, final String description);

    Optional<SchemaSummary> getSummary(final String schemaId);

    Optional<Schema> get(final String schemaId);

    Optional<SchemaSummary> updateDescription(final String schemaId, final String description);

    Optional<SchemaSummary> updateState(final String schemaId, final SchemaState state);

    Optional<FieldSchema> addField(final String schemaId, final FieldSchema schema);

    boolean delete(final String schemaId, final String fieldId);
}
