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
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Table(name = "schema_summaries")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoredSchemaSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @LookupKey
    @Column(name = "schema_id", nullable = false, length = 45, unique = true)
    private String schemaId;

    @Column(name = "name", nullable = false, length = 45)
    private String name;

    @Column(name = "description")
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private SchemaState state;

    @Column(name = "created_by", length = 45)
    private String createdBy;

    @Column(name = "state_changed_by", length = 45)
    private String stateChangedBy;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
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
