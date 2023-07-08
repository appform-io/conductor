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

package io.appform.conductor.server.workflowmanagement.impl.models;

import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.model.workflow.WorkflowState;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DB model for {@link io.appform.conductor.model.workflow.Workflow}
 */
@Entity
@Table(name = StoredWorkflow.WORKFLOW_TABLE_NAME)
@Getter
@Setter
@FieldNameConstants
@ToString
@NoArgsConstructor
public class StoredWorkflow implements Serializable {
    public static final String WORKFLOW_TABLE_NAME = "workflows";

    @Serial
    private static final long serialVersionUID = 7284973411073899897L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @LookupKey
    @Column(name = "workflow_id", unique = true)
    private String workflowId;

    @Column(name = "display_name")
    private String displayName;

    @Column
    private String description;

    @Column(name = "schema_id")
    private String schemaId;

    @Column(name = "start_state_id")
    private String startStateId;

    @Column
    @Enumerated(EnumType.STRING)
    private WorkflowState state;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Transient
    Map<String, TicketState> states;

    @Transient
    Map<String, List<TicketStateTransition>> ticketStateTransitions;

    @Transient
    Map<String, Rule> rules;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        StoredWorkflow that = (StoredWorkflow) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
