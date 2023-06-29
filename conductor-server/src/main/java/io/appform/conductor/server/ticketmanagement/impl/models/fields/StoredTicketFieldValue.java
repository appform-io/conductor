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

package io.appform.conductor.server.ticketmanagement.impl.models.fields;

import io.appform.conductor.model.schema.FieldType;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * DB model for {@link io.appform.conductor.model.ticket.fields.TicketField}
 */
@Entity
@Table(name = StoredTicketFieldValue.TICKET_FIELD_VALUE_BASE_TABLE_NAME/*,
        uniqueConstraints = @UniqueConstraint(name = "uk_ticket_field", columnNames = {"ticket_id", "schema_field_id"})*/)
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class StoredTicketFieldValue {
    public static final String TICKET_FIELD_VALUE_BASE_TABLE_NAME = "ticket_field_values_base";

    @Id
    @Column(name = "field_value_id", unique = true)
    private String fieldValueId;

    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "schema_field_id")
    private String schemaFieldId;

    @Column
    private final FieldType type;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredTicketFieldValue that = (StoredTicketFieldValue) o;
        return Objects.equals(getFieldValueId(), that.getFieldValueId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public abstract <T> T accept(final StoredTicketFieldValueVisitor<T> visitor);
}
