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
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.model.workflow.WorkflowState;
import io.appform.conductor.server.utils.Constants;
import io.appform.conductor.server.utils.persistence.StringListConverter;
import io.appform.conductor.server.utils.persistence.TemplateConverter;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = StoredWorkflow.WORKFLOW_TABLE_NAME,
uniqueConstraints = {
        @UniqueConstraint(name = "uk_workflow_id", columnNames = "workflow_id")
})
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
    @Column(name = "workflow_id", nullable = false, unique = true, length = Constants.MAX_WORKFLOW_ID_LENGTH)
    private String workflowId;

    @Column(name = "display_name", length = Constants.MAX_WORKFLOW_ID_LENGTH)
    private String displayName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "schema_id", length = Constants.MAX_SCHEMA_ID_LENGTH)
    private String schemaId;

    @Convert(converter = TemplateConverter.class)
    @Column(name = "title_template", columnDefinition = "longtext")
    private Template titleTemplate;

    @Convert(converter = TemplateConverter.class)
    @Column(name = "description_template", columnDefinition = "longtext")
    private Template descriptionTemplate;

    @Convert(converter = TemplateConverter.class)
    @Column(name = "subject_id_template",  columnDefinition = "longtext")
    private Template subjectIdTemplate;

    @Column(name = "start_state_id", length = Constants.MAX_WORKFLOW_STATE_ID_LENGTH)
    private String startStateId;

    @Column(name = "available_actions", columnDefinition = "longtext")
    @Convert(converter = StringListConverter.class)
    private List<String> availableActions;

    @Column(name = "state", length = 45)
    @Enumerated(EnumType.STRING)
    private WorkflowState state;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    @Transient
    transient Map<String, TicketState> states;

    @Transient
    transient Map<String, List<TicketStateTransition>> ticketStateTransitions;

    @Transient
    transient Map<String, Rule> rules;

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
