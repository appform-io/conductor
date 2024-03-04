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

package io.appform.conductor.server.schemamanagement.impl.models;

import io.appform.conductor.model.schema.SchemaState;
import io.appform.conductor.server.utils.Constants;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = StoredSchemaSummary.SCHEMA_TABLE_NAME, uniqueConstraints = {
        @UniqueConstraint( name = "uk_schema_id", columnNames = "schema_id")
})
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredSchemaSummary {

    public static final String SCHEMA_TABLE_NAME = "schema_summaries";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @LookupKey
    @Column(name = "schema_id", nullable = false, length = Constants.MAX_SCHEMA_ID_LENGTH, unique = true)
    private String schemaId;

    @Column(name = "name", nullable = false, length = Constants.MAX_SCHEMA_ID_LENGTH)
    private String name;

    @Column(name = "description", length = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "state", length = 45)
    @Enumerated(EnumType.STRING)
    private SchemaState state;

    @Column(name = "created_by", length = Constants.MAX_USER_ID_LENGTH)
    private String createdBy;

    @Column(name = "state_changed_by", length = Constants.MAX_USER_ID_LENGTH)
    private String stateChangedBy;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    @Transient
    private List<StoredFieldSchema> fields;

    @SuppressWarnings("java:S107")
    public StoredSchemaSummary(
            String schemaId,
            String name,
            String description,
            SchemaState state,
            String stateChangedBy) {
        this.schemaId = schemaId;
        this.name = name;
        this.description = description;
        this.state = state;
        this.stateChangedBy = stateChangedBy;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
