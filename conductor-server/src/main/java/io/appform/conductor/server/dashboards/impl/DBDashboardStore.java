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

package io.appform.conductor.server.dashboards.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.dashboards.DashboardStore;
import io.appform.conductor.server.dashboards.impl.model.StoredDashboard;
import io.appform.conductor.server.dashboards.model.Dashboard;
import io.appform.conductor.server.dashboards.model.DashboardSpec;
import io.appform.conductor.server.reporting.impl.models.StoredReport;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hibernate.criterion.DetachedCriteria;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 * DB based implementation for {@link DashboardStore}
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBDashboardStore implements DashboardStore {
    private final LookupDao<StoredDashboard> dashboardDao;
    private final ObjectMapper mapper;

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredDashboard.DASHBOARD_TABLE_NAME))
    public Optional<Dashboard> save(
            @Throws.RuntimeParam("id") String id,
            Dashboard dashboard) {
        return dashboardDao.createOrUpdate(id,
                                           existing -> addAttributes(existing, dashboard)
                                                   .setDeleted(false),
                                           () -> toStored(dashboard))
                .map(this::toWire);
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredDashboard.DASHBOARD_TABLE_NAME))
    public Optional<Dashboard> read(String id) {
        return dashboardDao.get(id)
                .map(this::toWire);
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredDashboard.DASHBOARD_TABLE_NAME))
    public boolean delete(String id) {
        return dashboardDao.delete(id);
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReport.REPORT_TABLE_NAME))
    public List<Dashboard> list() {
        return dashboardDao.scatterGather(DetachedCriteria.forClass(DashboardStore.class))
                .stream()
                .map(this::toWire)
                .toList();
    }

    private StoredDashboard toStored(final Dashboard dashboard) {
        return addAttributes(new StoredDashboard(), dashboard);
    }

    @SneakyThrows
    private StoredDashboard addAttributes(final StoredDashboard existing, final Dashboard dashboard) {
        return existing.setDashboardId(dashboard.getId())
                .setName(dashboard.getName())
                .setDescription(dashboard.getDescription())
                .setSpecVersion(dashboard.getSpecVersion())
                .setSpec(mapper.writeValueAsString(dashboard.getSpec()))
                .setLastUpdatedBy(ConductorServerUtils.operatingUserId());
    }

    @SneakyThrows
    private Dashboard toWire(final StoredDashboard dashboard) {
        return new Dashboard(dashboard.getDashboardId(),
                             dashboard.getName(),
                             dashboard.getDescription(),
                             dashboard.getSpecVersion(),
                             mapper.readValue(dashboard.getSpec(), DashboardSpec.class),
                             dashboard.getLastUpdatedBy(),
                             dashboard.getUpdated().getTime(),
                             dashboard.getUpdated());
    }
}
