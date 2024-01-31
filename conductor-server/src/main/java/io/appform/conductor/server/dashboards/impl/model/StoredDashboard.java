/*
 * Copyright (c) 2023 santanu
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

package io.appform.conductor.server.dashboards.impl.model;

import io.appform.conductor.server.dashboards.model.SpecVersion;
import io.appform.conductor.server.utils.Constants;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 *
 */
@Entity
@Table(name = StoredDashboard.DASHBOARD_TABLE_NAME)
@Getter
@Setter
@ToString
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "update dashboards set deleted=true where dashboard_id=?")
public class StoredDashboard {
    public static final String DASHBOARD_TABLE_NAME = "dashboards";

    @Id
    @Column(name = "dashboard_id", nullable = false, length = Constants.MAX_DASHBOARD_ID_LENGTH)
    @LookupKey
    private String dashboardId;

    @Column(name = "name", length = Constants.MAX_DASHBOARD_ID_LENGTH)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "spec", columnDefinition = "longtext")
    private String spec;

    @Column(name = "spec_version", length = 45)
    @Enumerated(EnumType.STRING)
    private SpecVersion specVersion;

    @Column(name = "provisioned_by", length = Constants.MAX_USER_ID_LENGTH)
    String lastUpdatedBy;

    @Column(name = "deleted")
    boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;
}
