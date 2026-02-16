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

package io.appform.conductor.console.dashboards.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.console.dashboards.DashboardStore;
import io.appform.conductor.console.dashboards.impl.model.StoredDashboard;
import io.appform.conductor.console.dashboards.model.Dashboard;
import io.appform.conductor.console.dashboards.model.DashboardSpec;
import io.appform.conductor.console.dashboards.model.SpecVersion;
import io.appform.conductor.console.reporting.impl.models.StoredReport;
import io.appform.conductor.core.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
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
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredDashboard.DASHBOARD_TABLE_NAME))
    public Optional<Dashboard> create(
            @Throws.RuntimeParam("id") final String id,
            final String name,
            final String description) {
        return dashboardDao.createOrUpdate(id,
                                           existing -> initialize(name, description, existing),
                                           () -> initialize(name, description, new StoredDashboard()))
                .map(this::toWire);
    }


    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredDashboard.DASHBOARD_TABLE_NAME))
    public Optional<Dashboard> update(
            @Throws.RuntimeParam("id") final String id,
            final String description,
            final SpecVersion specVersion,
            final DashboardSpec spec) {
        val status = dashboardDao.update(id, existing -> existing
                .map(dashboard -> {
                    try {
                        return dashboard.setDescription(description)
                                .setSpecVersion(specVersion)
                                .setSpec(mapper.writeValueAsString(spec))
                                .setLastUpdatedBy(ConductorServerUtils.operatingUserId());
                    }
                    catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(null));
        if (status) {
            return read(id);
        }
        return Optional.empty();
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
        return dashboardDao.scatterGather(DetachedCriteria.forClass(StoredDashboard.class)
                                                  .add(Property.forName(StoredDashboard.Fields.deleted).eq(false)))
                .stream()
                .map(this::toWire)
                .toList();
    }

    @SneakyThrows
    private StoredDashboard initialize(String name, String description, StoredDashboard existing) {
        return existing
                .setDashboardId(ConductorServerUtils.lowerSnake(name))
                .setName(name)
                .setDescription(description)
                .setSpecVersion(Objects.requireNonNullElse(existing.getSpecVersion(), SpecVersion.V1))
                .setSpec(Objects.requireNonNullElseGet(existing.getSpec(), this::emptySpec))
                .setLastUpdatedBy(ConductorServerUtils.operatingUserId())
                .setDeleted(false);
    }

    @SneakyThrows
    private String emptySpec() {
        return mapper.writeValueAsString(new DashboardSpec(List.of()));
    }

    @SneakyThrows
    private Dashboard toWire(final StoredDashboard dashboard) {
        return new Dashboard(dashboard.getDashboardId(),
                             dashboard.getName(),
                             dashboard.getDescription(),
                             dashboard.getSpecVersion(),
                             null != dashboard.getSpec()
                             ? mapper.readValue(dashboard.getSpec(), DashboardSpec.class)
                             : null,
                             dashboard.getLastUpdatedBy(),
                             dashboard.getUpdated().getTime(),
                             dashboard.getUpdated());
    }
}
