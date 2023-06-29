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

package io.appform.conductor.server.ticketmanagement.impl.models;

import io.appform.conductor.model.schema.FieldType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = StoredTicketFieldChoiceValue.TICKET_FIELD_CHOICE_VALUE_TABLE_NAME)
@Getter
@Setter
@ToString(callSuper = true)
public class StoredTicketFieldChoiceValue extends StoredTicketFieldValue {
    public static final String TICKET_FIELD_CHOICE_VALUE_TABLE_NAME = "ticket_field_choice_values";

    @Column(name = "choices_value")
    @Convert(converter = ChoicesStringConverter.class)
    private List<String> value;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredTicketFieldChoiceValue() {
        super(FieldType.CHOICE);
    }

    @Override
    public <T> T accept(StoredTicketFieldValueVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
