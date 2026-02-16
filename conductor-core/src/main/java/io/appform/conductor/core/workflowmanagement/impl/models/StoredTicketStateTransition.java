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

package io.appform.conductor.core.workflowmanagement.impl.models;

import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.model.workflow.TicketStateTransition;
import io.appform.conductor.core.utils.Constants;
import io.appform.conductor.core.utils.persistence.RuleConverter;
import io.appform.conductor.core.utils.persistence.StringListConverter;
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
import java.util.Objects;

/**
 * DB representation for {@link io.appform.conductor.model.workflow.TicketStateTransition}
 */
@Entity
@Table(name = StoredTicketStateTransition.WF_TRANSITIONS_TABLE_NAME,
        indexes = {
            @Index(name = "idx_workflow", columnList = "workflow_id")
})
@Getter
@Setter
@ToString
@FieldNameConstants
@NoArgsConstructor
public class StoredTicketStateTransition implements Serializable {
    public static final String WF_TRANSITIONS_TABLE_NAME = "workflow_state_transitions";

    @Serial
    private static final long serialVersionUID = -1633061170565671403L;

    @Id
    @Column(name = "transition_id", nullable = false, unique = true, length = Constants.MAX_WORKFLOW_TRANSITION_ID_LENGTH)
    private String transitionId;

    @Column(name = "from_state", length = Constants.MAX_WORKFLOW_STATE_ID_LENGTH)
    private String fromState;

    @Column(name = "to_state", length = Constants.MAX_WORKFLOW_STATE_ID_LENGTH)
    private String toState;

    @Column(name = "type", length = 45)
    @Enumerated(EnumType.STRING)
    private TicketStateTransition.TicketStateTransitionType type;

    @SuppressWarnings("java:S1948")
    @Convert(converter = RuleConverter.class)
    @Column(name = "rule", columnDefinition = "text", length = 10240)
    private Rule rule;

    @Convert(converter = StringListConverter.class)
    @Column(name = "action_id", columnDefinition =  "text", length = 10240)
    private List<String> actionIds;

    @Column
    private boolean deleted;

    @Column(name = "workflow_id", length = Constants.MAX_WORKFLOW_ID_LENGTH)
    private String workflowId;

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
        StoredTicketStateTransition that = (StoredTicketStateTransition) o;
        return Objects.equals(getTransitionId(), that.getTransitionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
