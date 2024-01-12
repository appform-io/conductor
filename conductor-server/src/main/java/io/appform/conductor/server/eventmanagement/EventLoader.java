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
import io.appform.conductor.model.events.EventSubType;
import io.dropwizard.lifecycle.Managed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.reflections.Reflections;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Loads all event types at startup
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Slf4j
@Order(20)
public class EventLoader implements Managed {
    private final Reflections reflections;
    private final ObjectMapper mapper;

    @Override
    public void start() throws Exception {
        val events = reflections.getTypesAnnotatedWith(EventSubType.class);
        events.forEach(eventType -> {
            val name = eventType.getAnnotation(EventSubType.class).value().name();
            mapper.registerSubtypes(
                    new NamedType(eventType, name));
            log.info("Event - {} ({})", eventType.getSimpleName(), name);
        });
    }
}
