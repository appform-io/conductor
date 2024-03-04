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

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.reporting.ReportState;
import io.appform.conductor.server.utils.Constants;
import io.appform.conductor.server.utils.persistence.StringListConverter;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = StoredReport.REPORT_TABLE_NAME)
@Getter
@Setter
@ToString
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "update reports set deleted=true where report_id=?")
public class StoredReport implements Serializable {
    public static final String REPORT_TABLE_NAME = "reports";

    @Serial
    private static final long serialVersionUID = -1739422964464502132L;

    @Id
    @Column(nullable = false, name =  "report_id", length = Constants.MAX_REPORT_ID_LENGTH)
    @LookupKey
    private String reportId;

    @Column(name = "name", length = Constants.MAX_REPORT_ID_LENGTH)
    private String name;

    @Column(name = "description", length = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "cql_query", length = Constants.MAX_CQL_LENGTH)
    String cqlQuery;

    @Column(name = "recipients", length = Constants.MAX_RECIPIENTS_LENGTH)
    @Convert(converter = StringListConverter.class)
    @SuppressWarnings("java:S1948")
    List<String> recipients;

    @Column(name = "cron", nullable = false, length = Constants.MAX_CRON_LENGTH)
    private String cron;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 45)
    ReportState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", length = 45)
    private Scope.ScopeType scopeType;

    @Column(name = "scope_reference_id", length = 255)
    private String scopeReferenceId;

    @Column(name = "provisioned_by", length = Constants.MAX_USER_ID_LENGTH)
    String provisionedBy;

    @Column(name = "deleted")
    boolean deleted;

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
        StoredReport that = (StoredReport) o;
        return Objects.equals(getReportId(), that.getReportId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
