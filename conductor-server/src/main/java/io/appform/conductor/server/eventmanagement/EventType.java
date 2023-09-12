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

package io.appform.conductor.server.eventmanagement;

import java.util.EnumSet;
import java.util.Set;

/**
 * Type of events
 */
public enum EventType {
    ROLE_CREATED,
    ROLE_MODIFIED,
    ROLE_DELETED,
    GROUP_CREATED,
    GROUP_UPDATED,
    GROUP_DELETED,
    USER_CREATED,
    USER_STATE_CHANGED,
    USER_ROLE_ASSIGNED,
    USER_GROUP_ASSIGNED,
    USER_GROUP_UNASSIGNED,
    SKILL_CREATED,
    SKILL_UPDATED,
    SKILL_DELETED,
    SKILL_VALUE_ADDED,
    SKILL_VALUE_REMOVED,
    USER_SKILL_ASSOCIATED,
    USER_SKILL_DISACCOSIATED,
    ;
    public static final Set<EventType> ALL = EnumSet.allOf(EventType.class);
}
