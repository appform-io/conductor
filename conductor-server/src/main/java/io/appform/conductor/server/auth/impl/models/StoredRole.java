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

package io.appform.conductor.server.auth.impl.models;

import io.appform.conductor.model.auth.Permission;
import io.appform.conductor.server.utils.persistence.PermissionsConverter;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
@Entity
@Table(name = StoredRole.ROLES_TABLE_NAME)
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredRole implements Serializable {
    public static final String ROLES_TABLE_NAME = "roles";

    @Serial
    private static final long serialVersionUID = 3350870151031507009L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @LookupKey
    @Column(name = "role_id", nullable = false, unique = true)
    private String roleId;

    @Column
    String name;

    @Column
    String description;

    @SuppressWarnings("java:S1948")
    @Column
    @Convert(converter = PermissionsConverter.class)
    Set<Permission> permissions;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredRole that = (StoredRole) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
