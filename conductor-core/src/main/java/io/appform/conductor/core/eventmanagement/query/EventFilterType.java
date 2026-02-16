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

package io.appform.conductor.core.eventmanagement.query;

import io.appform.conductor.model.events.Event;

/**
 * Type of filters that can be applied while querying for {@link Event}
 * from {@link io.appform.conductor.server.eventmanagement.EventStore}
 */
public enum EventFilterType {
    MATCH_ID,
    MATCH_REFERENCES,
    MATCH_USERS,
    MATCH_DATE_RANGE
}
