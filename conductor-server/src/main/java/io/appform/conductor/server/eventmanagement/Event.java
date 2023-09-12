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

import lombok.Data;

import java.util.Date;

import static io.appform.conductor.server.utils.ConductorServerUtils.operatingUserId;

/**
 * Base class for events
 */
@Data
public abstract class Event {
    private final EventType type;
    private final String userId;
    private final Date date;

    protected Event(EventType type) {
        this.type = type;
        this.userId = operatingUserId();
        this.date = new Date();
    }
    
    public abstract <T> T accept(final EventVisitor<T> visitor);
}
