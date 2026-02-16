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

package io.appform.conductor.server.dashboards.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.console.dashboards.model.DashboardRow;
import io.appform.conductor.console.dashboards.model.DashboardSpec;
import io.appform.conductor.console.dashboards.model.DashboardWidget;
import io.appform.conductor.core.utils.ConductorServerUtils;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 *
 */
class DashboardTest {

    @Test
    @SneakyThrows
    void test() {
        val mapper = new ObjectMapper();
        ConductorServerUtils.configureMapper(mapper);
        val out = mapper//.writerWithDefaultPrettyPrinter()
                .writeValueAsString(new DashboardSpec(
                        List.of(
                                new DashboardRow(List.of(new DashboardWidget("test",
                                                                             "Test Widget",
                                                                             DashboardWidget.QueryType.CQL,
                                                                             "select * from events where last('7d') group by time_bucket(date, 'HOUR')",
                                                                             12,
                                                                             Map.of()))))));
        System.out.println(out);
    }

}