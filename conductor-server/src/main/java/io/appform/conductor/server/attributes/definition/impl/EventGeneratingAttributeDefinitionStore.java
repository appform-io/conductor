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

package io.appform.conductor.server.attributes.definition.impl;

import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.events.impl.attributes.AttributeDefinitionDeletedEvent;
import io.appform.conductor.model.events.impl.attributes.AttributeDefinitionSavedEvent;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.attributes.definition.AttributeDefinitionStore;
import io.appform.conductor.server.eventmanagement.EventBus;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@Singleton
public class EventGeneratingAttributeDefinitionStore implements AttributeDefinitionStore {
    private final EventBus eventBus;
    private final AttributeDefinitionStore root;

    @Inject
    public EventGeneratingAttributeDefinitionStore(
            EventBus eventBus,
            @Named(ConductorModule.CACHED_IMPLEMENTATION_NAME) AttributeDefinitionStore root) {
        this.eventBus = eventBus;
        this.root = root;
    }

    @Override
    public Optional<AttributeDefinition> save(
            AttributeScopeType scopeType,
            String attributeDefinitionId,
            AttributeDefinition definition) {
        val status = root.save(scopeType, attributeDefinitionId, definition);
        if(status.isPresent()) {
            eventBus.publish(new AttributeDefinitionSavedEvent(attributeDefinitionId, scopeType));
        }
        return status;
    }

    @Override
    public List<AttributeDefinition> readAll(AttributeScopeType scopeType) {
        return root.readAll(scopeType);
    }

    @Override
    public Optional<AttributeDefinition> read(AttributeScopeType scopeType, String attributeDefinitionId) {
        return root.read(scopeType, attributeDefinitionId);
    }

    @Override
    public boolean delete(AttributeScopeType scopeType, String attributeDefinitionId) {
        val status = root.delete(scopeType, attributeDefinitionId);
        if(status) {
            eventBus.publish(new AttributeDefinitionDeletedEvent(attributeDefinitionId, scopeType));
        }
        return status;
    }
}
