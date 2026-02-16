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

package io.appform.conductor.core.ticketmanagement.impl.models;

import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.core.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.core.utils.Constants;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Storage model for {@link io.appform.conductor.server.ticketmanagement.TicketSkeleton}
 */
@Entity
@Table(name = StoredTicketSkeleton.TICKET_SKELETON_TABLE_NAME, indexes = {
        @Index(name = "idx_workflow_id", columnList = "workflow_id"),
        @Index(name = "idx_created_by_user_id", columnList = "created_by_user_id"),
        @Index(name = "idx_assigned_to_group_id", columnList = "assigned_to_group_id"),
        @Index(name = "idx_assigned_to_user_id", columnList = "assigned_to_user_id"),
        @Index(name = "idx_subject_id", columnList = "subject_id"),
        @Index(name = "idx_ext_ref", columnList = "ext_ref_source,ext_ref_id"),
        @Index(name = "idx_ticket_state_id", columnList = "ticket_state_id"),
        @Index(name = "idx_priority", columnList = "priority"),
        @Index(name = "idx_created", columnList = "created"),
        @Index(name = "idx_updated", columnList = "updated"),
})
@DynamicUpdate
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredTicketSkeleton implements Serializable {

    public static final String TICKET_SKELETON_TABLE_NAME = "ticket_skeletons";

    @Serial
    private static final long serialVersionUID = -9138428302273551724L;


    @Id
    @LookupKey
    @Column(name = "ticket_id", nullable = false, unique = true, length = Constants.MAX_TICKET_ID_LENGTH)
    private String ticketId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", length = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "workflow_id", nullable = false, length = Constants.MAX_WORKFLOW_ID_LENGTH)
    private String workflowId;

    @Column(name = "created_by_user_id", length = Constants.MAX_USER_ID_LENGTH)
    private String createdByUserId;

    @Column(name = "assigned_to_group_id", length = Constants.MAX_GROUP_ID_LENGTH)
    private String assignedToGroupId;

    @Column(name = "assigned_to_user_id", length = Constants.MAX_USER_ID_LENGTH)
    private String assignedToUserId;

    @Column(name = "subject_id", length = Constants.MAX_SUBJECT_GLOBAL_ID_LENGTH)
    private String subjectId;

    @Column(name = "ticket_state_id", length = Constants.MAX_WORKFLOW_STATE_ID_LENGTH)
    private String ticketStateId;

    @Column(name = "priority", length = 45)
    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    @Column(name = "ext_ref_source", length = 127)
    private String externalReferenceSource;

    @Column(name = "ext_ref_id",  length = 127)
    private String externalReferenceId;

    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<StoredFieldValue> fields;

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
        StoredTicketSkeleton that = (StoredTicketSkeleton) o;
        return Objects.equals(getTicketId(), that.getTicketId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
