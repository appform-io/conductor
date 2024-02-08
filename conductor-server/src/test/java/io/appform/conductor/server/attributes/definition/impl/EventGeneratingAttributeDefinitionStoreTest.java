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

import com.google.common.collect.Sets;
import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.attributes.definition.impl.StringAttributeDefinition;
import io.appform.conductor.model.events.EventType;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.attributes.definition.impl.models.StoredAttributeDefinition;
import io.appform.conductor.server.eventmanagement.bus.SignalDrivenEventBus;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.appform.conductor.model.attributes.AttributeScopeType.SUBJECT;
import static io.appform.conductor.model.attributes.AttributeScopeType.USER;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.attributes.definition.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
class EventGeneratingAttributeDefinitionStoreTest {

    @Test
    void testCRUD(
            final BalancedDBShardingBundle<TestConfig> bundle,
            final HazelcastClient hazelcastClient) {
        val eventBus = new SignalDrivenEventBus(Executors.newSingleThreadExecutor());
        record EventData(EventType eventType, String referredObjectId) {}
        val events = new TreeSet<EventData>(Comparator.comparing(EventData::eventType)
                                                    .thenComparing(EventData::referredObjectId));
        eventBus.register(event -> events.add(new EventData(event.getType(), event.getObjectId())));
        val store = new EventGeneratingAttributeDefinitionStore(
                eventBus,
                new CachingAttributeDefinitionStore(
                        new DBAttributeDefinitionStore(bundle.createRelatedObjectDao(StoredAttributeDefinition.class)),
                        hazelcastClient));
        val strAttrDef = new StringAttributeDefinition("S1",
                                                       "StrAttr",
                                                       "String Attribute",
                                                       "",
                                                       null,
                                                       null,
                                                       256,
                                                       "[0-9a-zA-Z]+");
        val saved = store.save(USER,
                               strAttrDef.getId(),
                               strAttrDef)
                .orElse(null);
        assertNotNull(saved);
        assertInstanceOf(StringAttributeDefinition.class, saved);
        assertEquals(strAttrDef.getName(), saved.getName());
        assertEquals(strAttrDef.getMaxLength(), ((StringAttributeDefinition) saved).getMaxLength());
        assertEquals(strAttrDef.getPattern(), ((StringAttributeDefinition) saved).getPattern());

        val updated = store.save(USER,
                                 saved.getId(),
                                 saved.withDescription("Test description"))
                .orElse(null);
        assertNotNull(updated);
        assertEquals("Test description", updated.getDescription());
        assertEquals(updated, store.read(USER, updated.getId()).orElse(null));
        assertTrue(store.delete(USER, updated.getId()));
        assertNull(store.read(USER, updated.getId()).orElse(null));
        assertTrue(events.contains(new EventData(EventType.ATTRIBUTE_DEFINITION_SAVED, "S1")));
                val userAttrIds = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> new StringAttributeDefinition("USA" + i,
                                                             "StrAttr" + i,
                                                             "String Attribute " + i,
                                                             "",
                                                             null,
                                                             null,
                                                             256,
                                                             "[0-9a-zA-Z]+"))
                .map(attrrDef -> store.save(USER, attrrDef.getId(), attrrDef).orElse(null))
                .filter(Objects::nonNull)
                .map(AttributeDefinition::getId)
                .collect(Collectors.toUnmodifiableSet());
        val subjectAttrIds = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> new StringAttributeDefinition("SSA" + i,
                                                             "StrAttr" + i,
                                                             "String Attribute " + i,
                                                             "",
                                                             null,
                                                             null,
                                                             256,
                                                             "[0-9a-zA-Z]+"))
                .map(attrrDef -> store.save(SUBJECT, attrrDef.getId(), attrrDef).orElse(null))
                .filter(Objects::nonNull)
                .map(AttributeDefinition::getId)
                .collect(Collectors.toUnmodifiableSet());
        assertTrue(Sets.difference(userAttrIds,
                                   store.readAll(USER)
                                           .stream()
                                           .map(AttributeDefinition::getId)
                                           .collect(Collectors.toUnmodifiableSet()))
                           .isEmpty());
        assertTrue(Sets.difference(subjectAttrIds,
                                   store.readAll(SUBJECT)
                                           .stream()
                                           .map(AttributeDefinition::getId)
                                           .collect(Collectors.toUnmodifiableSet()))
                           .isEmpty());
    }

/*    @Test
    void testMulti(
            final BalancedDBShardingBundle<TestConfig> bundle,
            final HazelcastClient hazelcastClient) {
        val store = new CachingAttributeDefinitionStore(
                new DBAttributeDefinitionStore(bundle.createRelatedObjectDao(StoredAttributeDefinition.class)),
                hazelcastClient);


    }*/
}