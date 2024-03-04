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

import com.google.common.util.concurrent.MoreExecutors;
import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.definition.impl.*;
import io.appform.conductor.model.attributes.value.impl.*;
import io.appform.conductor.model.events.impl.attributes.AttributeValueSavedEvent;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.attributes.values.impl.models.StoredAttributeValue;
import io.appform.conductor.server.eventmanagement.bus.SignalDrivenEventBus;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * High level test for {@link EventGeneratingAttributeValueStoreTest}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.attributes.values.impl.models")
@ExtendWith(DBTestExtension.class)
class EventGeneratingAttributeValueStoreTest {

    @SneakyThrows
    @Test
    void test(BalancedDBShardingBundle<TestConfig> bundle) {
        val eventBus = new SignalDrivenEventBus(MoreExecutors.newDirectExecutorService());
        val store = new EventGeneratingAttributeValueStore(
                eventBus,
                new DBAttributeValueStore(bundle.createRelatedObjectDao(StoredAttributeValue.class)));
        record EventData(AttributeScopeType scopeType, String objRefId) {
        }
        val eventCtr = new TreeMap<EventData, AtomicInteger>(
                Comparator.comparing(EventData::scopeType)
                        .thenComparing(EventData::objRefId));
        eventBus.register(event -> {
            final var dataKey = new EventData(((AttributeValueSavedEvent) event).getScopeType(), event.getObjectId());
            eventCtr.computeIfAbsent(
                            dataKey,
                            key -> new AtomicInteger(0))
                    .incrementAndGet();
        });
        val userAttrDefs = Map.of(
                "ugender", new ChoiceAttributeDefinition("ugender",
                                                         "gender",
                                                         "Gender",
                                                         "",
                                                         null,
                                                         null,
                                                         Set.of("MALE", "FEMALE", "OTHER"),
                                                         false),
                "udob", new DateAttributeDefinition("udob",
                                                    "dob",
                                                    "Date Of Birth",
                                                    "",
                                                    null,
                                                    null),
                "uAltGuardian", new StringAttributeDefinition("uAltGuardian",
                                                              "guardian",
                                                              "Guardian",
                                                              "",
                                                              null,
                                                              null,
                                                              255,
                                                              "[a-zA-Z0-9 ]+"),
                "uAge", new NumberAttributeDefinition("uAge",
                                                      "age",
                                                      "Age",
                                                      "Description",
                                                      null,
                                                      null,
                                                      0.0,
                                                      150.0),
                "uLink", new LinkAttributeDefinition("uLink",
                                                     "link",
                                                     "Link",
                                                     "",
                                                     null,
                                                     null)
                                 );
        val userId = "U001";
        {
            val res = store.save(AttributeScopeType.USER,
                                 userId,
                                 List.of(new NumberAttributeValue("uAge", 45))
                                );
            assertEquals(1, res.size());
            assertEquals(45, ((NumberAttributeValue) res.get(0)).getValue());
        }
        {
            val res = store.save(AttributeScopeType.USER,
                                 userId,
                                 List.of(new ChoiceAttributeValue("uGender", List.of("OTHER")))
                                );
            assertEquals(2, res.size());
            assertEquals(45, ((NumberAttributeValue) res.get(0)).getValue());
            assertEquals("OTHER", ((ChoiceAttributeValue) res.get(1)).getValue().get(0));
        }
        { //Test Update
            val res = store.save(AttributeScopeType.USER,
                                 userId,
                                 List.of(new ChoiceAttributeValue("uGender", List.of("MALE")))
                                );
            assertEquals(2, res.size());
            assertEquals(45, ((NumberAttributeValue) res.get(0)).getValue());
            assertEquals("MALE", ((ChoiceAttributeValue) res.get(1)).getValue().get(0));
        }
        {
            val dob = new GregorianCalendar(1975, Calendar.JANUARY, 12).getTime();
            val res = store.save(AttributeScopeType.USER,
                                 userId,
                                 List.of(new DateAttributeValue("udob", dob))
                                );
            assertEquals(3, res.size());
            assertEquals(45, ((NumberAttributeValue) res.get(0)).getValue());
            assertEquals("MALE", ((ChoiceAttributeValue) res.get(1)).getValue().get(0));
            assertEquals(dob, ((DateAttributeValue) res.get(2)).getValue());
        }
        {
            val res = store.save(AttributeScopeType.USER,
                                 userId,
                                 List.of(new StringAttributeValue("uAltGuardian", "Some Other"))
                                );
            assertEquals(4, res.size());
            assertEquals("Some Other", ((StringAttributeValue) res.get(3)).getValue());
        }
        {
            val res = store.save(AttributeScopeType.USER,
                                 userId,
                                 List.of(new LinkAttributeValue("uLink",
                                                                "Last Test Results",
                                                                "https://blah.com/1234"))
                                );
            assertEquals(5, res.size());
            assertEquals("Last Test Results", ((LinkAttributeValue) res.get(4)).getText());
            assertEquals("https://blah.com/1234",
                         ((LinkAttributeValue) res.get(4)).getValue());
            assertEquals(res, store.read(AttributeScopeType.USER, userId));
        }
        assertEquals(6, eventCtr.get(new EventData(AttributeScopeType.USER, userId)).get());



    }
}