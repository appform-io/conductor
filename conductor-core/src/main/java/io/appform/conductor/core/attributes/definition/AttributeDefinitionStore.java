/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.core.attributes.definition;

import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.attributes.AttributeScopeType;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface AttributeDefinitionStore {
    Optional<AttributeDefinition> save(
            final AttributeScopeType scopeType,
            final String attributeDefinitionId,
            final AttributeDefinition definition);

    List<AttributeDefinition> readAll(final AttributeScopeType scopeType);

    Optional<AttributeDefinition> read(final AttributeScopeType scopeType, final String attributeDefinitionId);

    boolean delete(final AttributeScopeType scopeType, final String attributeDefinitionId);
}
