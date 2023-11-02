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

package io.appform.conductor.server.reporting.impl.models;

import io.appform.conductor.model.reporting.ReportRun;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SQLDelete;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredReportRun.REPORT_RUN_TABLE_NAME,
        indexes = {
            @Index(name = "report_run_report_id_idx", columnList = "report_id"),
            @Index(name = "report_run_run_date_idx", columnList = "run_date")
        }

)
@Getter
@Setter
@ToString
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "update report_runs set deleted=true where id=?")
public class StoredReportRun implements Serializable {
    public static final String REPORT_RUN_TABLE_NAME = "report_runs";

    @Serial
    private static final long serialVersionUID = 3448236804714127769L;

    @Id
    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "report_id")
    private String reportId;

    @Column(name = "run_date")
    private Date runDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state")
    ReportRun.State currentState;

    @Column
    private String message;

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
        StoredReportRun that = (StoredReportRun) o;
        return Objects.equals(getRunId(), that.getRunId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
