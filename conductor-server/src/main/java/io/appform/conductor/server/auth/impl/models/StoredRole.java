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
import io.appform.conductor.server.utils.Constants;
import io.appform.conductor.server.utils.persistence.PermissionsConverter;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;

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
@SQLDelete(sql = "update roles set deleted=true where role_id=?")
public class StoredRole implements Serializable {
    public static final String ROLES_TABLE_NAME = "roles";

    @Serial
    private static final long serialVersionUID = 3350870151031507009L;

    @Id
    @LookupKey
    @Column(name = "role_id", nullable = false, unique = true, length = Constants.MAX_ROLE_ID_LENGTH)
    private String roleId;

    @Column(name = "name", length = Constants.MAX_ROLE_ID_LENGTH)
    String name;

    @Column(name = "description", length = Constants.MAX_DESCRIPTION_LENGTH)
    String description;

    @SuppressWarnings("java:S1948")
    @Column(name = "permissions", columnDefinition = "text", length = 10240)
    @Convert(converter = PermissionsConverter.class)
    Set<Permission> permissions;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
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
        return Objects.equals(getRoleId(), that.getRoleId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
