/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.server.attributes.values.impl.models;

import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.AttributeType;
import io.appform.conductor.server.schemamanagement.impl.models.StoredFieldSchema;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SQLDelete;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredAttributeValue.ATTRIBUTE_VALUES_TABLE,
        indexes = {
                @Index(name = "idx_attr_val_scope", columnList = "scope_type,object_ref_id")
        })
@DiscriminatorColumn(name = StoredAttributeValue.Fields.type)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Getter
@Setter
@ToString
@FieldNameConstants
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "update attribute_values set deleted=true where attribute_def_id=?")
public abstract class StoredAttributeValue implements Serializable {
    public static final String ATTRIBUTE_VALUES_TABLE = "attribute_values";

    @Serial
    private static final long serialVersionUID = 2037183699403892876L;

    @Id
    @Column(name = "attribute_value_id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(insertable = false, updatable = false)
    private final AttributeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type")
    private AttributeScopeType scopeType;

    @Column(name = "object_ref_id")
    private String objectRefId;

    @Column(name = "attr_def_id")
    private String attrDefId;

    @Column(name = "attribute_id")
    private String attributeId;

    @Column
    boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp on update current_timestamp",
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
        val that = (StoredFieldSchema) o;
        return Objects.equals(getId(), that.getFieldId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public abstract <T> T accept(final StoredAttributeValueVisitor<T> visitor);
}
