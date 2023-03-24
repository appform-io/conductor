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

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface SchemaStore {
    Optional<Schema> create(final String name, final String description, final List<FieldSchema> fields);

    Optional<Schema> get(final String schemaId);

    Optional<Schema> getVersion(final String schemaId, final long version);

    Optional<Schema> updateDescription(final String schemaId, final long version, final String description);

    Optional<Schema> updateState(
            final String schemaId,
            final long version,
            final SchemaState required,
            final SchemaState newState);

    Optional<Schema> updateFields(final String schemaId, final long version, final List<FieldSchema> fields);

}
