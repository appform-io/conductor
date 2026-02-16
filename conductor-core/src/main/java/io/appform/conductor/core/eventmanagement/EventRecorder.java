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

package io.appform.conductor.core.eventmanagement;

import io.appform.conductor.model.events.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
@EventHandlerImplementation
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class EventRecorder implements EventHandler {
    private final EventStore eventStore;

    @Override
    public void handle(Event event) {
        try {
            eventStore.save(event.getId(), event);
        }
        catch (Throwable t) {
            log.error("Error saving event " + event.getId() + "/" + event.getType(), t);
        }
    }
}
