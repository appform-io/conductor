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

import io.appform.conductor.model.schema.FieldType;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SQLDelete;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * DB model for {@link io.appform.conductor.model.schema.FieldSchema}
 */
@Entity
@Table(name = "field_schemas")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "update field_schemas set deleted=true where field_id=?")
public abstract class StoredFieldSchema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private final FieldType type;

    @Column(name = "schema_id", nullable = false)
    private String schemaId;

    @Column(name = "field_id", unique = true, nullable = false)
    private String fieldId;

    @Column(name = "name")
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "required")
    private boolean required;

    @Column(name = "parent_id")
    private String parent;

    @Column(name = "visibility_condition")
    private String visibilityCondition;

    @Column(name = "editable_condition")
    private String editableCondition;

    @Column(name = "allow_multiple")
    private boolean allowMultiple;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public abstract <T> T accept(final StoredFieldSchemaVisitor<T> visitor);

    @SuppressWarnings("java:S107")
    protected StoredFieldSchema(FieldType type,
                                String schemaId,
                                String fieldId, String name,
                                String displayName,
                                String description,
                                boolean required,
                                String parent,
                                String visibilityCondition,
                                String editableCondition,
                                boolean allowMultiple) {
        this.type = type;
        this.schemaId = schemaId;
        this.fieldId = fieldId;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.required = required;
        this.parent = parent;
        this.visibilityCondition = visibilityCondition;
        this.editableCondition = editableCondition;
        this.allowMultiple = allowMultiple;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        val that = (StoredFieldSchema) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
