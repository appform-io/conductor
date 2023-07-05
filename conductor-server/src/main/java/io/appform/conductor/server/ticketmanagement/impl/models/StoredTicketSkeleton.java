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

package io.appform.conductor.server.ticketmanagement.impl.models;

import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Storage model for {@link io.appform.conductor.server.ticketmanagement.TicketSkeleton}
 */
@Entity
@Table(name = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME)
@DynamicUpdate
@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoredTicketSkeleton implements Serializable {

    public static final String TICKET_SUMMARY_TABLE_NAME = "ticket_skeletons";
    @Serial
    private static final long serialVersionUID = -9138428302273551724L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @LookupKey
    @Column(name = "ticket_id", unique = true, length = 45)
    private String ticketId;

    @Column
    private String title;

    @Column
    private String description;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "created_by_user_id")
    private String createdByUserId;

    @Column(name = "assigned_to_group_id")
    private String assignedToGroupId;

    @Column(name = "assigned_to_user_id")
    private String assignedToUserId;

    @Column(name = "subject_id")
    private String subjectId;

    @Column(name = "ticket_state_id")
    private String ticketStateId;

    @Column
    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<StoredFieldValue> fields;

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
        StoredTicketSkeleton that = (StoredTicketSkeleton) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
