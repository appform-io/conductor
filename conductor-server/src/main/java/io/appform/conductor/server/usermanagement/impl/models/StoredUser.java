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

import io.appform.conductor.model.usermgmt.UserState;
import io.appform.conductor.model.usermgmt.UserSummary;
import io.appform.conductor.model.usermgmt.UserType;
import io.appform.conductor.server.usermanagement.impl.DBUserStore;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;

/**
 * DB model object corresponding to {@link UserSummary}
 */
@Entity
@Table(name = DBUserStore.USER_TABLE_NAME)
@Data
@NoArgsConstructor
public class StoredUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @LookupKey
    @Column(name = "user_id", unique = true, nullable = false, length = 45)
    private String userId;

    @Column(name = "user_type", unique = false, nullable = false, length = 45)
    private UserType userType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column
    @Enumerated(EnumType.STRING)
    private UserState state;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    public StoredUser(
            String userId,
            UserType userType,
            String name,
            String email,
            UserState state) {
        this.userId = userId;
        this.userType = userType;
        this.name = name;
        this.email = email;
        this.state = state;
    }
}
