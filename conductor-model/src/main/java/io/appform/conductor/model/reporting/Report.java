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

package io.appform.conductor.model.reporting;

import io.appform.conductor.model.actions.Scope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Date;
import java.util.List;

/**
 *
 */
@Value
@AllArgsConstructor
@Builder
@Jacksonized
public class Report {
    String id;
    String name;
    String description;
    String cqlQuery;
    List<String> recipients;
    String cron;
    ReportState state;
    Scope scope;
    String provisionedBy;
    Date created;
    Date updated;
}
