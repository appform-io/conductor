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

package io.appform.conductor.core.eventmanagement.bus;

import io.appform.conductor.model.events.Event;
import io.appform.conductor.core.eventmanagement.EventHandler;
import io.appform.conductor.model.events.EventType;
import io.appform.signals.signalhandlers.SignalConsumer;

import java.util.Set;

/**
 * Filters and dispatches only relevant events
 */
public class FilteringEventConsumer implements SignalConsumer<Event> {
    private final Set<EventType> eventTypes;
    private final EventHandler root;

    public FilteringEventConsumer(EventHandler root) {
        this.root = root;
        this.eventTypes = Set.copyOf(root.listenFor());
    }

    @Override
    public void consume(Event event) {
        if(eventTypes.contains(event.getType())) {
            root.handle(event);
        }
    }
}
