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

package io.appform.conductor.server.reporting;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.reporting.Report;
import io.appform.conductor.model.reporting.ReportRun;
import io.appform.conductor.model.reporting.ReportRunResult;

import java.util.*;

/**
 *
 */
public interface ReportStore {
    Optional<Report> save(
            String id,
            String name,
            String description,
            String cql,
            List<String> emails,
            String cron,
            Scope scope);

    Optional<Report> get(String id);

    boolean delete(String id);

    List<Report> listReports();

    Optional<ReportRun> scheduleRun(String reportId);

    List<ReportRun> runs(String reportId, final Collection<ReportRun.State> states);

    List<ReportRun> relevantRuns(
            final String reportId,
            final Date maxDate,
            final Collection<ReportRun.State> states);

    List<ReportRun> runsForStates(ReportRun.State state);

    void markCompleted(String reportId,
                       String runId,
                       ReportRunResult runResult);

}
