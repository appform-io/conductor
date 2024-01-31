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

import io.appform.conductor.server.utils.Constants;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredUserPassword.USER_PASSWORD_TABLE_NAME)
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredUserPassword {
    public static final String USER_PASSWORD_TABLE_NAME = "user_passwords";

    @Id
    @LookupKey
    @Column(name = "user_id", unique = true, nullable = false, length = Constants.MAX_USER_ID_LENGTH)
    private String userId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "failed_password_attempt")
    private int failedPasswordAttempts;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    public StoredUserPassword(String userId, String password, int failedPasswordAttempts) {
        this.userId = userId;
        this.password = password;
        this.failedPasswordAttempts = failedPasswordAttempts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredUserPassword that = (StoredUserPassword) o;
        return Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getPassword(), that.getPassword()) ;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
