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

import io.appform.conductor.model.usermgmt.UserActivationToken;
import io.appform.conductor.model.usermgmt.UserActivationTokenState;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * DB model for {@link UserActivationToken}
 */
@Entity
@Table(name = StoredUserActivationToken.ACTIVATION_TOKEN_TABLE_NAME,
        indexes = @Index(name = "idx_tokens_for_user", columnList = "user_id"))
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredUserActivationToken {
    public static final String ACTIVATION_TOKEN_TABLE_NAME = "user_activation_links";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @LookupKey
    @Column(name = "token", unique = true, nullable = false, length = 45)
    private String token;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "valid_till", unique = true, nullable = false)
    private Date validTill;

    @Column(name = "partitionId", nullable = false)
    private int partitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 45)
    private UserActivationTokenState state;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false,
            insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredUserActivationToken(
            String token,
            String userId,
            Date validTill,
            UserActivationTokenState state) {
        this.token = token;
        this.userId = userId;
        this.validTill = validTill;
        this.state = state;
        this.partitionId = ConductorServerUtils.currentWeek();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredUserActivationToken that = (StoredUserActivationToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
