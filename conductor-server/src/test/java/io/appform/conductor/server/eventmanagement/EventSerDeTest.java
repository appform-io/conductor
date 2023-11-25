/*
 * Copyright (c) 2023 santanu
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

package io.appform.conductor.server.eventmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.EventSubType;
import io.appform.conductor.model.reporting.ReportRun;
import io.appform.conductor.model.reporting.ReportRunResult;
import io.appform.conductor.model.events.impl.reporting.ReportExecutionCompletedEvent;
import io.appform.conductor.server.utils.ConductorServerUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
@Slf4j
class EventSerDeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void configure() {
        ConductorServerUtils.configureMapper(MAPPER);
        val reflections = new Reflections("io.appform.conductor.model.events");
        val events = reflections.getTypesAnnotatedWith(EventSubType.class);
        events.forEach(eventType -> {
            val name = eventType.getAnnotation(EventSubType.class).value().name();
            MAPPER.registerSubtypes(
                    new NamedType(eventType, name));
            log.info("Event - {} ({})", eventType.getSimpleName(), name);
        });
    }

    @Test
    @SneakyThrows
    void testSerDe() {
        val event = new ReportExecutionCompletedEvent("r1",
                                                      new ReportRunResult("r1",
                                                                          "rr1",
                                                                          ReportRun.State.FINISHED,
                                                                          "Done"));
        val data = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(event);
        System.out.println(data);
        assertEquals(event, MAPPER.readValue(data, Event.class));
    }
}