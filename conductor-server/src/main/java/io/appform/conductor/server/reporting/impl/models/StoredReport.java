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
import io.appform.conductor.server.utils.persistence.StringListConverter;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SQLDelete;

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
    @Column(nullable = false, name =  "report_id")
    @LookupKey
    private String reportId;

    @Column
    private String name;

    @Column
    private String description;

    @Column(name = "cql_query")
    String cqlQuery;

    @Column(name = "recipients")
    @Convert(converter = StringListConverter.class)
    @SuppressWarnings("java:S1948")
    List<String> recipients;

    @Column(name = "cron", nullable = false)
    private String cron;

    @Enumerated(EnumType.STRING)
    @Column
    ReportState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type")
    private Scope.ScopeType scopeType;

    @Column(name = "scope_reference_id")
    private String scopeReferenceId;

    @Column(name = "provisioned_by")
    String provisionedBy;

    @Column
    boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp on update current_timestamp",
            updatable = false, insertable = false)
    @org.hibernate.annotations.Generated(value = GenerationTime.ALWAYS)
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
