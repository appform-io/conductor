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

import com.google.common.collect.Table;
import io.appform.conductor.server.eventmanagement.query.EventFilters;
import io.appform.conductor.server.eventmanagement.query.EventListResult;

/**
 * A storage system for events
 */
public interface EventStore {
    boolean save(String eventId, final Event event);

    EventListResult list(EventFilters filters, String nextPointer, int size);

    Table<Integer, String, Object> groupBy(EventFilters filters);
}
