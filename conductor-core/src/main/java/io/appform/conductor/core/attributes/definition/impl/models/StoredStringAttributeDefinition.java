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

package io.appform.conductor.core.attributes.definition.impl.models;

import io.appform.conductor.model.attributes.AttributeType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Objects;

/**
 *
 */
@Entity
@Getter
@Setter
@DiscriminatorValue(AttributeType.Values.STRING_TYPE)
@ToString(callSuper = true)
public class StoredStringAttributeDefinition extends StoredAttributeDefinition {
    @Column(name = "max_length")
    private int maxLength;

    @Column
    private String pattern;

    public StoredStringAttributeDefinition() {
        super(AttributeType.STRING);
    }

    @Override
    public <T> T accept(StoredAttributeDefinitionVisitor<T> visitor) {
        return visitor.visit(this);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        val that = (StoredStringAttributeDefinition) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
