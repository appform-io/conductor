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

package io.appform.conductor.server.taskmanagement.impl.models;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.server.taskmanagement.ConductorTaskScheduler;
import io.appform.conductor.server.taskmanagement.model.TaskState;
import io.appform.conductor.server.taskmanagement.model.TaskType;
import io.appform.conductor.server.utils.Constants;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredTask.TASK_TABLE_NAME, indexes = {
        @Index(name = "idx_scope_reference_id", columnList = "scope_reference_id")
})
@Getter
@Setter
@ToString
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class StoredTask implements Serializable {
    public static final String TASK_TABLE_NAME = "tasks";

    @Serial
    private static final long serialVersionUID = -6360713282136062573L;

    @Id
    @LookupKey
    @Column(nullable = false, name = "task_id", length = Constants.MAX_TASK_ID_LENGTH)
    private String taskId;

    @Column(name = "type", length = 45)
    @Enumerated(EnumType.STRING)
    private TaskType type;

    @Column(name = "name", length = Constants.MAX_TASK_ID_LENGTH)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "execution_interval_ms")
    private long interval;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", length = 45)
    private Scope.ScopeType scopeType;

    @Column(name = "scope_reference_id", length = 255)
    private String scopeReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 45)
    private TaskState state;

    @Column(name = "last_execution_time")
    private Date lastExecutionCompletionTime;

    @Column(name = "last_run_status", length = 45)
    private ConductorTaskScheduler.TaskStatus lastRunStatus;

    @Column(name = "task_data", columnDefinition = "longtext")
    private String spec;

    @Column(name = "task_meta", columnDefinition = "longtext")
    private String taskMeta;

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
        val that = (StoredTask) o;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
