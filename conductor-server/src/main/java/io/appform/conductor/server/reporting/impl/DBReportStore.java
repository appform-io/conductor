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

package io.appform.conductor.server.reporting.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.reporting.Report;
import io.appform.conductor.model.reporting.ReportRun;
import io.appform.conductor.model.reporting.ReportRunResult;
import io.appform.conductor.model.reporting.ReportState;
import io.appform.conductor.server.reporting.ReportContext;
import io.appform.conductor.server.reporting.ReportStore;
import io.appform.conductor.server.reporting.impl.models.StoredReport;
import io.appform.conductor.server.reporting.impl.models.StoredReportContext;
import io.appform.conductor.server.reporting.impl.models.StoredReportRun;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_RELATED_ENTITY_LIST_ERROR;
import static io.appform.conductor.server.utils.ConductorServerUtils.nextExecutionTimeForCron;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DBReportStore implements ReportStore {

    private final LookupDao<StoredReport> reportDao;
    private final RelationalDao<StoredReportContext> reportContextDao;
    private final RelationalDao<StoredReportRun> reportRunDao;
    private final ObjectMapper mapper;

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReport.REPORT_TABLE_NAME))
    public Optional<Report> save(
            @Throws.RuntimeParam("id") String id,
            String name,
            String description,
            String cql,
            List<String> emails,
            String cron,
            Scope scope) {
        val report = reportDao.createOrUpdate(id,
                                              existing -> existing.setDescription(description)
                                                      .setCqlQuery(cql)
                                                      .setRecipients(emails)
                                                      .setCron(cron)
                                                      .setScopeType(scope.getType())
                                                      .setScopeReferenceId(scope.getReferenceId())
                                                      .setDeleted(false),
                                              () -> new StoredReport()
                                                      .setReportId(id)
                                                      .setName(name)
                                                      .setDescription(description)
                                                      .setCqlQuery(cql)
                                                      .setRecipients(emails)
                                                      .setCron(cron)
                                                      .setState(ReportState.ACTIVE)
                                                      .setProvisionedBy(ConductorServerUtils.operatingUserId())
                                                      .setScopeType(scope.getType())
                                                      .setScopeReferenceId(scope.getReferenceId()))
                .orElse(null);
        Preconditions.checkNotNull(report);
        return Optional.ofNullable(reportDao.lockAndGetExecutor(id)
                                           .update(reportRunDao,
                                                   DetachedCriteria.forClass(StoredReportRun.class)
                                                           .add(Property.forName(StoredReportRun.Fields.reportId)
                                                                        .eq(id))
                                                           .add(Property.forName(StoredReportRun.Fields.currentState)
                                                                        .eq(ReportRun.State.SCHEDULED)),
                                                   run -> run.setCurrentState(ReportRun.State.CANCELLED)
                                                           .setMessage("Run cancelled as report was updated"),
                                                   () -> true)
                                           .save(reportRunDao, DBReportStore::newRunForReport)
                                           .execute())
                .map(DBReportStore::toWire);
    }

    @Override
    public Optional<Report> updateState(String reportId, ReportState state) {
        if (reportDao.update(reportId,
                             existingOptional -> existingOptional.map(existing -> existing.setState(state))
                                     .orElse(null))) {
            val report = switch (state) {
                case ACTIVE -> reportDao.lockAndGetExecutor(reportId)
                        .update(reportRunDao,
                                DetachedCriteria.forClass(StoredReportRun.class)
                                        .add(Property.forName(StoredReportRun.Fields.reportId)
                                                     .eq(reportId))
                                        .add(Property.forName(StoredReportRun.Fields.currentState)
                                                     .eq(ReportRun.State.SCHEDULED)),
                                run -> run.setCurrentState(ReportRun.State.CANCELLED)
                                        .setMessage("Run cancelled as report was activated."),
                                () -> true)
                        .save(reportRunDao, DBReportStore::newRunForReport)
                        .execute();
                case DISABLED -> reportDao.lockAndGetExecutor(reportId)
                        .update(reportRunDao,
                                DetachedCriteria.forClass(StoredReportRun.class)
                                        .add(Property.forName(StoredReportRun.Fields.reportId)
                                                     .eq(reportId))
                                        .add(Property.forName(StoredReportRun.Fields.currentState)
                                                     .eq(ReportRun.State.SCHEDULED)),
                                run -> run.setCurrentState(ReportRun.State.CANCELLED)
                                        .setMessage("Run cancelled as report was deactivated"),
                                () -> true)
                        .execute();
            };
            return Optional.of(toWire(report));
        }
        return Optional.empty();
    }

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReport.REPORT_TABLE_NAME))
    public Optional<Report> get(String id) {
        return reportDao.get(id)
                .filter(storedReport -> !storedReport.isDeleted())
                .map(DBReportStore::toWire);
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReport.REPORT_TABLE_NAME))
    public boolean delete(@Throws.RuntimeParam("id") String id) {
        val report = reportDao.lockAndGetExecutor(id)
                .update(reportRunDao,
                        DetachedCriteria.forClass(StoredReportRun.class)
                                .add(Property.forName(StoredReportRun.Fields.reportId)
                                             .eq(id))
                                .add(Property.forName(StoredReportRun.Fields.currentState)
                                             .eq(ReportRun.State.SCHEDULED)),
                        run -> run.setCurrentState(ReportRun.State.CANCELLED)
                                .setMessage("Run cancelled as report was deleted")
                                .setDeleted(true),
                        () -> true)
                .update(reportContextDao,
                        DetachedCriteria.forClass(StoredReportContext.class)
                                .add(Property.forName(StoredReportRun.Fields.reportId)
                                             .eq(id)),
                        context -> context.setDeleted(true),
                        () -> true)
                .execute();
        if (null != report) {
            return reportDao.delete(id);
        }
        return false;
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReport.REPORT_TABLE_NAME))
    public List<Report> listReports() {
        return reportDao.scatterGather(DetachedCriteria.forClass(StoredReport.class)
                                               .add(Property.forName(StoredReport.Fields.deleted).eq(false)))
                .stream()
                .map(DBReportStore::toWire)
                .toList();
    }

    @Override
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReportRun.REPORT_RUN_TABLE_NAME))
    public List<ReportRun> runs(
            @Throws.RuntimeParam("id") String reportId, Collection<ReportRun.State> states) {
        return reportRunDao.select(reportId, DetachedCriteria.forClass(StoredReportRun.class)
                                           .add(Property.forName(StoredReportRun.Fields.currentState).in(states)),
                                   0,
                                   Integer.MAX_VALUE)
                .stream()
                .map(this::toWire)
                .toList();
    }

    @Override
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReportRun.REPORT_RUN_TABLE_NAME))
    public List<ReportRun> relevantRuns(
            @Throws.RuntimeParam("id") String reportId,
            Date maxDate,
            Collection<ReportRun.State> states,
            int size) {
        val criteria = DetachedCriteria.forClass(StoredReportRun.class)
                .add(Property.forName(StoredReportRun.Fields.reportId).eq(reportId));
        if (null != maxDate) {
            criteria.add(Property.forName(StoredReportRun.Fields.runDate).lt(maxDate));
        }
        if (null != states && !states.isEmpty()) {
            criteria.add(Property.forName(StoredReportRun.Fields.currentState).in(states));
        }
        criteria.addOrder(Order.desc(StoredReportRun.Fields.runDate));
        return reportRunDao.select(reportId,
                                   criteria,
                                   0,
                                   Integer.MAX_VALUE)
                .stream()
                .map(this::toWire)
                .sorted(Comparator.comparing(ReportRun::getRunDate).reversed())
                .limit(size)
                .toList();
    }

    @Override
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReportRun.REPORT_RUN_TABLE_NAME))
    public List<ReportRun> runsForStates(ReportRun.State state) {
        return reportRunDao.scatterGather(DetachedCriteria.forClass(StoredReportRun.class)
                                                  .add(Property.forName(StoredReportRun.Fields.currentState).eq(state)),
                                          0,
                                          Integer.MAX_VALUE)
                .stream()
                .map(this::toWire)
                .toList();
    }

    @Override
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReportRun.REPORT_RUN_TABLE_NAME))
    public void markCompleted(
            @Throws.RuntimeParam("id") String reportId,
            @Throws.RuntimeParam("subId") String runId,
            ReportRunResult runResult,
            ReportContext updatedContext) {
        reportDao.lockAndGetExecutor(reportId)
                .update(reportRunDao, DetachedCriteria.forClass(StoredReportRun.class)
                                .add(Property.forName(StoredReportRun.Fields.reportId).eq(reportId))
                                .add(Property.forName(StoredReportRun.Fields.runId).eq(runId)),
                        run -> run.setCurrentState(runResult.getRunState())
                                .setMessage(runResult.getMessage()),
                        () -> false)
                .save(reportRunDao, DBReportStore::newRunForReport)
                .createOrUpdate(reportContextDao,
                                DetachedCriteria.forClass(StoredReportContext.class)
                                        .add(Property.forName(StoredReportContext.Fields.reportId).eq(reportId)),
                                existing -> existing.setReportData(serializeReportMeta(updatedContext.getData()))
                                        .setDeleted(false),
                                () -> new StoredReportContext()
                                        .setReportId(reportId)
                                        .setReportData(serializeReportMeta(updatedContext.getData())))
                .execute();

    }


    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredReport.REPORT_TABLE_NAME))
    public Optional<ReportContext> readContext(@Throws.RuntimeParam("id") String reportId) {
        return reportContextDao.select(reportId, DetachedCriteria.forClass(StoredReportContext.class)
                                               .add(Property.forName(StoredReportContext.Fields.reportId).eq(reportId)),
                                       0,
                                       1)
                .stream()
                .findAny()
                .map(this::toWire);
    }

    private static StoredReportRun newRunForReport(StoredReport report) {
        val runDate = nextExecutionTimeForCron(report.getReportId(), report.getCron(), new Date());
        val nextRunId = "RR-" + UUID.nameUUIDFromBytes((report.getReportId()
                + "-" + runDate.getTime()
                + "-" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        return new StoredReportRun()
                .setRunId(nextRunId)
                .setReportId(report.getReportId())
                .setRunDate(runDate)
                .setCurrentState(ReportRun.State.SCHEDULED);
    }

    private static Report toWire(final StoredReport report) {
        return new Report(
                report.getReportId(),
                report.getName(),
                report.getDescription(),
                report.getCqlQuery(),
                report.getRecipients(),
                report.getCron(),
                report.getState(),
                Scope.create(report.getScopeType(), report.getScopeReferenceId()),
                report.getProvisionedBy(),
                report.getCreated(),
                report.getUpdated()
        );
    }

    private ReportRun toWire(final StoredReportRun reportRun) {
        return new ReportRun(
                reportRun.getRunId(),
                reportRun.getReportId(),
                reportRun.getRunDate(),
                reportRun.getCurrentState(),
                reportRun.getMessage()
        );
    }

    private ReportContext toWire(final StoredReportContext context) {
        return new ReportContext(context.getReportId(), deserializeReportMeta(context.getReportData()));
    }

    @SneakyThrows
    private Map<String, Object> deserializeReportMeta(String reportMetaString) {
        return Strings.isNullOrEmpty(reportMetaString)
               ? Map.of()
               : mapper.readValue(reportMetaString, new TypeReference<>() {
               });
    }


    @SneakyThrows
    private String serializeReportMeta(Map<String, Object> reportMeta) {
        return mapper.writeValueAsString(reportMeta);
    }
}
