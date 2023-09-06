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

package io.appform.conductor.server.skillmanagement.impl.models;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredSkillValue.SKILL_VALUE_TABLE_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
@FieldNameConstants
public class StoredSkillValue implements Serializable {
    public static final String SKILL_VALUE_TABLE_NAME = "skill_values";

    @Serial
    private static final long serialVersionUID = -4529613046616172870L;

    @Id
    @Column(name = "value_id", length = 45, nullable = false, unique = true)
    private String valueId;

    @Column(name = "skill_id", length = 45, nullable = false)
    private String skillId;

    @Column(name = "skill_value", nullable = false)
    private String value;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy
                                   ? hibernateProxy.getHibernateLazyInitializer()
                                           .getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = o instanceof HibernateProxy hibernateProxy
                                      ? hibernateProxy.getHibernateLazyInitializer()
                                              .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        val that = (StoredSkillValue) o;
        return Objects.equals(getValueId(), that.getValueId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy
               ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
               : getClass().hashCode();
    }
}
