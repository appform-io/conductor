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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.server.eventmanagement.events.ReferredObjectType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import static io.appform.conductor.server.utils.ConductorServerUtils.operatingUserId;

/**
 * Base class for events
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class Event {

    @Value
    @Builder
    public static class EventTime {
        int second;
        int minute;
        int hour;
        int day;
        int month;
        int year;
    }

    @JsonProperty("type")
    private final EventType type;
    private final String id;
    private final ReferredObjectType objectType;
    private final String objectId;
    private final String userId;
    private final Date date;
    private final EventTime eventTime;

    protected Event(EventType type, ReferredObjectType objectType, String objectId) {
        this(type,
             UUID.randomUUID().toString(),
             objectType,
             objectId,
             operatingUserId(),
             new Date(),
             eventTime(new Date()));
    }

    public abstract <T> T accept(final EventVisitor<T> visitor);

    private static EventTime eventTime(Date date) {
        val now = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return EventTime.builder()
                .second(now.getSecond())
                .minute(now.getMinute())
                .hour(now.getHour())
                .day(now.getDayOfMonth())
                .month(now.getMonth().getValue())
                .year(now.getYear())
                .build();
    }
}
