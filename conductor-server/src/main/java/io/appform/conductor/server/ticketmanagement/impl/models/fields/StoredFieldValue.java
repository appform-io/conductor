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
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredFieldValue.TICKET_FIELD_VALUE_TABLE_NAME/*,
        uniqueConstraints = @UniqueConstraint(name = "uk_ticket_field", columnNames = {"ticket_id", "schema_field_id"})*/)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredFieldValue implements Serializable {
    public static final String TICKET_FIELD_VALUE_TABLE_NAME = "ticket_field_values";
    @Serial
    private static final long serialVersionUID = 7147859251068889312L;

    @Id
    @Column(name = "field_value_id", unique = true)
    private String fieldValueId;

    @ManyToOne
    @JoinColumn(name = "ticket_id", referencedColumnName="ticket_id")
    private StoredTicketSkeleton ticket;

    @Column(name = "schema_field_id")
    private String schemaFieldId;

    @Column
    @Enumerated(EnumType.STRING)
    private FieldType type;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "boolean_value")
    private boolean booleanValue;

    @Column(name = "number_value")
    private double numberValue;

    @Column(name = "location_lat_value")
    private double locationLatValue;

    @Column(name = "location_lon_value")
    private double locationLonValue;

    @Column(name = "choices_value")
    @Convert(converter = ChoicesStringConverter.class)
    private List<String> choiceValue;

    @Column(name = "date_value")
    private Date dateValue;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredFieldValue that = (StoredFieldValue) o;
        return Objects.equals(getFieldValueId(), that.getFieldValueId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
