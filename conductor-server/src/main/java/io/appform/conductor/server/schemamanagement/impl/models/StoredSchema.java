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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;

import static io.appform.conductor.server.schemamanagement.impl.DBSchemaStore.SCHEMA_TABLE_NAME;

/**
 *
 */
@Entity
@Table(name = SCHEMA_TABLE_NAME, uniqueConstraints = {
        @UniqueConstraint(name = "uk_schema_id_version", columnNames = {"schema_id", "version"})
})
@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoredSchema {


    public enum DefinitionType {
        RAW,
        COMPRESSED_ZLIB
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "schema_id", nullable = false, length = 45)
    private String schemaId;

    @Column(name = "name", nullable = false, length = 45)
    private String name;

    @Column(name = "description")
    private String description;

    @Column
    private long version;

    @Column(columnDefinition = "varbinary")
    private byte[] fields;

    @Column(name = "definition_type")
    @Enumerated(EnumType.STRING)
    private DefinitionType definitionType;

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

    @SuppressWarnings("java:S107")
    public StoredSchema(
            String schemaId,
            String name,
            String description,
            long version,
            byte[] fields,
            DefinitionType definitionType,
            SchemaState state,
            String stateChangedBy) {
        this.schemaId = schemaId;
        this.name = name;
        this.description = description;
        this.version = version;
        this.fields = fields;
        this.definitionType = definitionType;
        this.state = state;
        this.stateChangedBy = stateChangedBy;
    }
}
