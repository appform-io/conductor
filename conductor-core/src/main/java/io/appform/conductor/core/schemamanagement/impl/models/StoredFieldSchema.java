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

package io.appform.conductor.core.schemamanagement.impl.models;

import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.core.utils.persistence.RuleConverter;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;
import java.util.Objects;

/**
 * DB model for {@link io.appform.conductor.model.schema.FieldSchema}
 */
@Entity
@Table(name = "field_schemas", indexes = {
        @Index(name = "idx_schema_id", columnList = "schema_id"),
})
@DiscriminatorColumn(name = StoredFieldSchema.Fields.type)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Getter
@Setter
@ToString
@FieldNameConstants
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "update field_schemas set deleted=true where field_id=?")
public abstract class StoredFieldSchema {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, insertable = false, updatable = false, length = 45)
    private final FieldType type;

    @Column(name = "schema_id", nullable = false, length = Constants.MAX_SCHEMA_ID_LENGTH)
    private String schemaId;

    @Id
    @Column(name = "field_id", unique = true, nullable = false, length = Constants.MAX_FIELD_ID_LENGTH)
    private String fieldId;

    @Column(name = "name", length = Constants.MAX_FIELD_NAME_LENGTH)
    private String name;

    @Column(name = "display_name", length = 45)
    private String displayName;

    @Column(name = "description", length = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "parent_id", length = 255)
    private String parent;

    @Convert(converter = RuleConverter.class)
    @Column(name = "visibility_condition", columnDefinition = "text", length = 10240)
    private Rule visibilityCondition;

    @Convert(converter = RuleConverter.class)
    @Column(name = "editable_condition", columnDefinition =  "text", length = 10240)
    private Rule editableCondition;

    @Column(name = "allow_multiple")
    private boolean allowMultiple;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    public abstract <T> T accept(final StoredFieldSchemaVisitor<T> visitor);

    @SuppressWarnings("java:S107")
    protected StoredFieldSchema(final FieldType type,
                                final String schemaId,
                                final String fieldId, String name,
                                final String displayName,
                                final String description,
                                final String parent,
                                final Rule visibilityCondition,
                                final Rule editableCondition,
                                final boolean allowMultiple) {
        this.type = type;
        this.schemaId = schemaId;
        this.fieldId = fieldId;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
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
        return Objects.equals(getFieldId(), that.getFieldId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
