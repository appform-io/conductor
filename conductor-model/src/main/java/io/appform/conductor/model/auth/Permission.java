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

package io.appform.conductor.model.auth;

import io.appform.conductor.model.utils.Displayable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 *
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Permission implements Displayable {
    ADMIN(Values.ADMIN, "Administration tasks"),
    MANAGE_SCHEMA(Values.MANAGE_SCHEMA, "Manage ticket schema"),
    MANAGE_WORKFLOW(Values.MANAGE_WORKFLOW, "Manage ticket workflows"),
    MANAGE_GROUPS(Values.MANAGE_GROUPS, "Manage user groups"),
//    MANAGE_ACTIONS(Values.MANAGE_ACTIONS, "Manage ticket actions"),
    MANAGE_REPORT(Values.MANAGE_REPORT, "Manage scheduled reports"),
    MANAGE_DASHBOARD(Values.MANAGE_DASHBOARD, "Manage dashboards"),
    MANAGE_ATTRIBUTE_DEFINITIONS(Values.MANAGE_ATTRIBUTE_DEFINITIONS, "Manage attribute definitions"),
//    MANAGE_TASK(Values.MANAGE_TASK, "Manage scheduled tasks"),
    TICKET_READ(Values.TICKET_READ, "See ticket details"),
    TICKET_WRITE(Values.TICKET_WRITE, "Create/Update a ticket")
    ;

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Values {
        public static final String ADMIN = "ADMIN";
        public static final String MANAGE_SCHEMA = "MANAGE_SCHEMA";
        public static final String MANAGE_WORKFLOW = "MANAGE_WORKFLOW";
        public static final String MANAGE_GROUPS = "MANAGE_GROUPS";
        /*public static final String MANAGE_ACTIONS = "MANAGE_ACTIONS";
        public static final String MANAGE_TASK = "MANAGE_TASK";*/
        public static final String MANAGE_REPORT = "MANAGE_REPORT";
        public static final String MANAGE_DASHBOARD = "MANAGE_DASHBOARD";
        public static final String MANAGE_ATTRIBUTE_DEFINITIONS = "MANAGE_ATTRIBUTE_DEFINITIONS";
        public static final String TICKET_READ = "TICKET_READ";
        public static final String TICKET_WRITE = "TICKET_WRITE";
    }

    private final String value;
    @Getter
    private final String displayText;

    @Override
    public String displayText() {
        return displayText;
    }

}
