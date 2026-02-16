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

package io.appform.conductor.console.reporting.impl.models;

import io.appform.conductor.model.reporting.ReportRun;
import io.appform.conductor.core.utils.Constants;
import lombok.*;
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
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredReportRun.REPORT_RUN_TABLE_NAME,
        indexes = {
            @Index(name = "idx_report_id", columnList = "report_id"),
            @Index(name = "idx_run_date", columnList = "run_date"),
            @Index(name = "idx_current_state", columnList = "current_state"),
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
    @Column(name = "run_id", nullable = false, length = Constants.MAX_REPORT_RUN_ID_LENGTH)
    private String runId;

    @Column(name = "report_id", length = Constants.MAX_REPORT_ID_LENGTH)
    private String reportId;

    @Column(name = "run_date")
    private Date runDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", length = 45)
    ReportRun.State currentState;

    @Column(name = "message", length = 255)
    private String message;

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
        StoredReportRun that = (StoredReportRun) o;
        return Objects.equals(getRunId(), that.getRunId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
