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

import io.appform.conductor.model.workflow.Rule;
import io.appform.conductor.model.workflow.TicketStateTransition;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * DB representation for {@link io.appform.conductor.model.workflow.TicketStateTransition}
 */
@Entity
@Table(name = StoredTicketStateTransition.WF_TRANSITIONS_TABLE_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoredTicketStateTransition implements Serializable {
    public static final String WF_TRANSITIONS_TABLE_NAME = "workflow_state_transitions";

    @Serial
    private static final long serialVersionUID = -1633061170565671403L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "ext_id", unique = true)
    private String extId;

    @Column(name = "from_state")
    private String fromState;

    @Column(name = "to_state")
    private String toState;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TicketStateTransition.TicketStateTransitionType type;

    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private Rule.RuleType ruleType;

    @Column(name = "rule")
    private String rule;

    @Column(name = "action_id")
    private String actionId;

    @Column
    private boolean deleted;

    @Column(name = "workflow_id")
    private String workflowId;
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
        StoredTicketStateTransition that = (StoredTicketStateTransition) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
