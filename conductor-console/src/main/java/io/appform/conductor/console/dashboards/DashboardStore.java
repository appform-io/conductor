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

package io.appform.conductor.server.dashboards;

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.dashboards.model.Dashboard;
import io.appform.conductor.server.dashboards.model.DashboardSpec;
import io.appform.conductor.server.dashboards.model.SpecVersion;
import io.appform.conductor.server.reporting.impl.models.StoredReport;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface DashboardStore {
    Optional<Dashboard> create(final String id, final String name, final String description);

    Optional<Dashboard> update(final String id,
                               final String description,
                               final SpecVersion specVersion,
                               final DashboardSpec spec);

    Optional<Dashboard> read(final String id);
    boolean delete(final String id);

    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReport.REPORT_TABLE_NAME))
    List<Dashboard> list();
}
