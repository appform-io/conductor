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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * DB representation for {@link io.appform.conductor.model.schema.TicketState}
 */
@Entity
@Table(name = StoredWorkflowSelectionRule.WF_SELECTION_RULE_TABLE_NAME,
        indexes = {
                @Index(name = "idx_wf_selection_rule_wf_id", columnList = "workflow_id"),
        }
)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoredWorkflowSelectionRule implements Serializable {
    public static final String WF_SELECTION_RULE_TABLE_NAME = "workflow_selection_rules";
    @Serial
    private static final long serialVersionUID = -4729081581755725993L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "ext_id", unique = true)
    private String extId;

    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private Rule.RuleType ruleType;

    @Column(name = "rule")
    private String rule;

    @Column(name = "workflow_id")
    private String workflowId;

    @Column
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
        StoredWorkflowSelectionRule that = (StoredWorkflowSelectionRule) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
