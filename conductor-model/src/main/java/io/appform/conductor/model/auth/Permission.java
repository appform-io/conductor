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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Permission {
    ADMIN(Values.ADMIN),
    SCHEMA_READ(Values.SCHEMA_READ),
    SCHEMA_WRITE(Values.SCHEMA_WRITE),
    SCHEMA_APPROVE(Values.SCHEMA_APPROVE),
    WORKFLOW_READ(Values.WORKFLOW_APPROVE),
    WORKFLOW_WRITE(Values.WORKFLOW_WRITE),
    WORKFLOW_APPROVE(Values.WORKFLOW_APPROVE),
    ACTION_READ(Values.ACTION_READ),
    ACTION_WRITE(Values.ACTION_WRITE),
    TICKET_READ(Values.TICKET_READ),
    TICKET_WRITE(Values.TICKET_WRITE)
    ;

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Values {
        public static final String ADMIN = "ADMIN";
        public static final String SCHEMA_READ = "SCHEMA_READ";
        public static final String SCHEMA_WRITE = "SCHEMA_WRITE";
        public static final String SCHEMA_APPROVE = "SCHEMA_APPROVE";
        public static final String WORKFLOW_READ = "WORKFLOW_READ";
        public static final String WORKFLOW_WRITE = "WORKFLOW_WRITE";
        public static final String WORKFLOW_APPROVE = "WORKFLOW_APPROVE";
        public static final String ACTION_READ = "ACTION_READ";
        public static final String ACTION_WRITE = "ACTION_WRITE";
        public static final String TICKET_READ = "TICKET_READ";
        public static final String TICKET_WRITE = "TICKET_WRITE";
    }

    @Getter
    private final String value;

}
