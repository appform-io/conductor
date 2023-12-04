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

package io.appform.conductor.server.eventmanagement.store.models;

import io.appform.conductor.model.events.EventType;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.Date;

/**
 *
 */
@Entity
@Table(name = StoredEvent.EVENTS_TABLE_NAME,
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_id_partition", columnNames = {"id", "partition_id"})
        },
        indexes = {
                @Index(name = "idx_events_reference", columnList = "referred_object_type, referred_object_id"),
                @Index(name = "idx_events_user", columnList = "user_id"),
                @Index(name = "idx_events_date", columnList = "date"),
        })
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredEvent {
    public static final String EVENTS_TABLE_NAME = "events";

    public enum SourceFmt {
        V1, //Full data serialized and uncompressed
    }

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "referred_object_type")
    private ReferredObjectType objectType;

    @Column(name = "referred_object_id")
    private String objectId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "date", columnDefinition = "timestamp default current_timestamp", updatable = false,
            insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date date;

    @Column(columnDefinition = "longtext")
    private String source;

    @Column(columnDefinition = "char(3)", name = "source_fmt")
    @Enumerated(EnumType.STRING)
    private SourceFmt sourceFormat;

    @Column(name = "partition_id")
    private int partitionId;

    public StoredEvent(
            String id,
            EventType type,
            ReferredObjectType objectType,
            String objectId,
            String userId,
            Date date) {
        this.id = id;
        this.type = type;
        this.objectType = objectType;
        this.objectId = objectId;
        this.userId = userId;
        this.date = date;
        this.partitionId = Instant.now()
                .atZone(ZoneId.systemDefault())
                .get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
