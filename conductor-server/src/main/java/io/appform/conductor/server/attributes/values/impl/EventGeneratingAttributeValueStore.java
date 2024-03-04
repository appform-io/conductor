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

package io.appform.conductor.server.attributes.values.impl;

import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.value.AttributeValue;
import io.appform.conductor.model.events.impl.attributes.AttributeValueSavedEvent;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.attributes.values.AttributeValueStore;
import io.appform.conductor.server.eventmanagement.EventBus;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

/**
 * Generates events on updates
 */
@Singleton
public class EventGeneratingAttributeValueStore implements AttributeValueStore {
    private final EventBus eventBus;
    private final AttributeValueStore root;

    @Inject
    public EventGeneratingAttributeValueStore(
            EventBus eventBus,
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) AttributeValueStore root) {
        this.eventBus = eventBus;
        this.root = root;
    }

    @Override
    public List<AttributeValue> save(
            AttributeScopeType scopeType,
            String objectRefId,
            List<AttributeValue> attributes) {
        val res = root.save(scopeType, objectRefId, attributes);
        if(res.size() >= attributes.size()) {
            eventBus.publish(new AttributeValueSavedEvent(scopeType, objectRefId));
        }
        else {
            System.out.println("SKIPPING: " + attributes);
        }
        return res;
    }

    @Override
    public List<AttributeValue> read(AttributeScopeType scopeType, String objectRefId) {
        return root.read(scopeType, objectRefId);
    }
}
