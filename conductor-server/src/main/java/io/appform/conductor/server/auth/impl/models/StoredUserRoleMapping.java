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

/**
 *
 */
@Entity
@Table(name = StoredUserRoleMapping.USER_ROLE_MAPPING_TABLE_NAME,
        indexes = {
                @Index(name = "uk_user_role", columnList = "user_id", unique = true),
        })
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredUserRoleMapping implements Serializable {
    public static final String USER_ROLE_MAPPING_TABLE_NAME = "user_role_mappings";

    @Serial
    private static final long serialVersionUID = 8332369056839193904L;

    @Id
    @Column(name = "mapping_id", nullable = false)
    private String mappingId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "role_id", nullable = false)
    private String roleId;


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
        StoredUserRoleMapping that = (StoredUserRoleMapping) o;
        return Objects.equals(getMappingId(), that.getMappingId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
