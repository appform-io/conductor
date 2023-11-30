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

package io.appform.conductor.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.UUID;

/**
 * Base class for events
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@FieldNameConstants
public abstract class Event {

    @Value
    @Builder
    @Jacksonized
    @FieldNameConstants
    public static class EventTime {
        int millisecond;
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
    private String userId;
    private final Date date;
    private final EventTime eventTime;

    protected Event(EventType type, ReferredObjectType objectType, String objectId) {
        this(type,
             UUID.randomUUID().toString(),
             objectType,
             objectId,
             null, //Will be set during publication
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
                .millisecond(now.get(ChronoField.MILLI_OF_SECOND))
                .build();
    }
}
