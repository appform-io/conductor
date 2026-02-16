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

import io.appform.conductor.core.utils.Constants;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredReportContext.REPORT_CONTEXT_TABLE_NAME)
@Getter
@Setter
@ToString
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "update report_contexts set deleted=true where id=?")
public class StoredReportContext implements Serializable {
    public static final String REPORT_CONTEXT_TABLE_NAME = "report_contexts";

    @Serial
    private static final long serialVersionUID = 8272445945763996787L;

    @Id
    @Column(name = "report_id", length = Constants.MAX_REPORT_ID_LENGTH)
    private String reportId;

    @Column(name = "report_data", columnDefinition = "longtext", length = 65535)
    private String reportData;

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
        StoredReportContext that = (StoredReportContext) o;
        return Objects.equals(getReportId(), that.getReportId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
