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

package io.appform.conductor.user.usermanagement.impl.models;

import io.appform.conductor.model.usermgmt.SessionState;
import io.appform.conductor.model.usermgmt.SessionType;
import io.appform.conductor.core.utils.ConductorServerUtils;
import io.appform.conductor.core.utils.Constants;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * DB model object corresponding to {@link io.appform.conductor.model.usermgmt.UserSessionDetails}
 */
@Entity
@Table(name = StoredUserSessionDetails.SESSION_TABLE_NAME, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_id_session_id", columnNames = {"user_id", "session_id", "partition_id"})
})
@Data
@FieldNameConstants
@NoArgsConstructor
public class StoredUserSessionDetails {
    public static final String SESSION_TABLE_NAME = "user_sessions";

    @Id
    @Column(name = "session_id", nullable = false, length = Constants.MAX_SESSION_ID_LENGTH)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = Constants.MAX_USER_ID_LENGTH)
    private String userId;

    @Column(name = "state", length = 45)
    @Enumerated(EnumType.STRING)
    private SessionState state;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private SessionType type;

    @Column(name = "expiry")
    private Date expiry;

    @Column(name = "last_active")
    private Date lastActive;

    @Column(name = "partition_id", nullable = false)
    private int partitionId;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    public StoredUserSessionDetails(
            String sessionId,
            String userId,
            SessionState state,
            SessionType type,
            Date expiry,
            Date lastActive) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.state = state;
        this.type = type;
        this.expiry = expiry;
        this.lastActive = lastActive;
        this.partitionId = ConductorServerUtils.currentWeek();
    }
}
