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

import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.proxy.HibernateProxy;

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
@Table(name = StoredSkillDefinition.SKILL_DEFINITION_TABLE_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
@FieldNameConstants
public class StoredSkillDefinition implements Serializable {
    public static final String SKILL_DEFINITION_TABLE_NAME = "skill_definitions";

    @Serial
    private static final long serialVersionUID = 7473115434950219055L;

    @Id
    @Column(name = "skill_id", length = 45, nullable = false, unique = true)
    @LookupKey
    private String skillId;

    @Column(name = "name", nullable = false)
    private String name;

    @Transient
    private List<StoredSkillValue> values;

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
        val that = (StoredSkillDefinition) o;
        return Objects.equals(getSkillId(), that.getSkillId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy
               ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
               : getClass().hashCode();
    }
}
