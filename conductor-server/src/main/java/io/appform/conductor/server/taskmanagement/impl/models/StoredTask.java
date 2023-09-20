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
import io.appform.conductor.server.taskmanagement.model.TaskState;
import io.appform.conductor.server.taskmanagement.model.TaskType;
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
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredTask.TASK_TABLE_NAME)
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
    @Column(nullable = false)
    @LookupKey
    private String id;

    @Enumerated(EnumType.STRING)
    private TaskType type;

    @Column
    private String name;

    @Column
    private String description;

    @Column(name = "execution_interval_ms")
    private long interval;

    @Column(name = "scope_type")
    private Scope.ScopeType scopeType;

    @Column(name = "scope_reference_id")
    private String scopeReferenceId;

    @Enumerated(EnumType.STRING)
    @Column
    private TaskState state;

    @Column(name = "last_execution_time")
    private Date lastExecutionCompletionTime;

    @Column(name = "task_data", columnDefinition = "blob")
    private String spec;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp on update current_timestamp",
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
        val that = (StoredTask) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
