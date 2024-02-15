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

package io.appform.conductor.server.usermanagement.impl.models;

import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.server.utils.Constants;
import io.appform.conductor.server.utils.persistence.StringSetConverter;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
@Entity
@Table(name = StoredGroup.GROUP_TABLE_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
@FieldNameConstants
@SQLDelete(sql = "update user_groups set deleted=true where group_id=?")
public class StoredGroup {
    public static final String GROUP_TABLE_NAME = "user_groups";

    @Id
    @Column(name = "group_id", length = Constants.MAX_GROUP_ID_LENGTH, nullable = false, unique = true)
    @LookupKey
    private String groupId;

    @Column(name = "name", nullable = false, length = Constants.MAX_GROUP_ID_LENGTH)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "type", length = 45)
    @Enumerated(EnumType.STRING)
    private GroupType type;

    @Convert(converter = StringSetConverter.class)
    @Column(name = "required_skills", columnDefinition = "longtext")
    private Set<String> requiredSkills;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    public StoredGroup(String groupId, String name, String description, GroupType type, Set<String> requiredSkills) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.requiredSkills = requiredSkills;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                                   ? ((HibernateProxy) o).getHibernateLazyInitializer()
                                           .getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                                      ? ((HibernateProxy) this).getHibernateLazyInitializer()
                                              .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        StoredGroup that = (StoredGroup) o;
        return Objects.equals(getGroupId(), that.getGroupId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                .getPersistentClass()
                .hashCode() : getClass().hashCode();
    }
}
