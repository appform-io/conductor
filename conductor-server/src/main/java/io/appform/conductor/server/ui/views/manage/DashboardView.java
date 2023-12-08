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

package io.appform.conductor.server.ui.views.manage;

import io.appform.conductor.model.events.analytics.ObjectReference;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.server.dashboards.model.Dashboard;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 * Render a list of dashboards
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DashboardView extends BaseLoggedInView {
    Dashboard currentDashboard;

    public DashboardView(User currentUser, Dashboard currentDashboard) {
        super("templates/dashboard/dashboard.hbs",
              currentUser,
              new ObjectReference(ReferredObjectType.DASHBOARD, null));
        this.currentDashboard = currentDashboard;
    }
}
