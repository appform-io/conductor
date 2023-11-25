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

import com.google.common.base.Strings;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.reporting.Report;
import io.appform.conductor.model.reporting.ReportRun;
import io.appform.conductor.model.reporting.ReportRunResult;
import io.appform.conductor.model.reporting.ReportState;
import io.appform.conductor.model.ticket.analytics.TicketGroupResponse;
import io.appform.conductor.model.ticket.analytics.TicketListResponse;
import io.appform.conductor.model.ticket.analytics.TicketQueryResponse;
import io.appform.conductor.model.ticket.analytics.TicketQueryResponseVisitor;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.comms.MailSender;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.model.events.impl.reporting.ReportExecutionCompletedEvent;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.ticketmanagement.TicketManager;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.signals.signals.ScheduledSignal;
import io.dropwizard.lifecycle.Managed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static io.appform.conductor.server.utils.ConductorServerUtils.tabulate;

/**
 *
 */

@Singleton
@Slf4j
public class ReportManager implements Managed {
    private static final String HANDLER_NAME = "REPORT_POLLER";

    private final ReportStore reportStore;
    private final ScheduledSignal poller = new ScheduledSignal(java.time.Duration.ofMinutes(1));
    private final EventBus eventBus;
    private final CQLEngine cqlEngine;
    private final TicketManager ticketManager;
    private final MailSender mailSender;
    private final ExecutorService reportRunnerPool;

    private final Map<String, String> currentlyRunningReports = new ConcurrentHashMap<>();

    @Inject
    public ReportManager(
            ReportStore reportStore,
            EventBus eventBus,
            CQLEngine cqlEngine,
            TicketManager ticketManager,
            MailSender mailSender,
            @Named(ConductorModule.BACKGROUND_JOBS_POOL_NAME) ExecutorService reportRunnerPool) {
        this.reportStore = reportStore;
        this.eventBus = eventBus;
        this.cqlEngine = cqlEngine;


        this.ticketManager = ticketManager;
        this.mailSender = mailSender;
        this.reportRunnerPool = reportRunnerPool;
    }


    public Optional<Report> create(
            String id,
            String name,
            String description,
            String cql,
            List<String> emails,
            String cron,
            Scope scope) {
        val reportId = Strings.isNullOrEmpty(id)
                       ? ConductorServerUtils.lowerSnake(name)
                       : id;
        return reportStore.save(reportId,
                                name,
                                description,
                                cql,
                                emails,
                                cron,
                                scope);
    }

    public Optional<Report> update(
            String id,
            String description,
            String cql,
            List<String> emails,
            String cron,
            Scope scope) {
        return reportStore.save(id,
                                null, //This is not updated
                                description,
                                cql,
                                emails,
                                cron,
                                scope);
    }

    public Optional<Report> get(String id) {
        return reportStore.get(id);
    }

    public boolean delete(String id) {
        return reportStore.delete(id);
    }

    public Optional<Report> activate(String id) {
        return reportStore.activate(id);
    }

    public Optional<Report> deactivate(String id) {
        return reportStore.deactivate(id);
    }

    public List<Report> listReports() {
        return reportStore.listReports();
    }

    public List<ReportRun> runsForReport(
            final String reportId, int size) {
        return reportStore.runsForReport(reportId, size);
    }

    @Override
    public void start() throws Exception {
        poller.connect(HANDLER_NAME, date -> handleClockTick(reportStore,
                                                             eventBus,
                                                             cqlEngine,
                                                             ticketManager,
                                                             mailSender,
                                                             reportRunnerPool,
                                                             currentlyRunningReports,
                                                             date));
        log.info("Report polling handler connected");
    }

    @Override
    public void stop() throws Exception {
        poller.disconnect(HANDLER_NAME);
        poller.close();
        log.info("Report polling handler disconnected");
    }

    @SuppressWarnings("java:S107")
    private static void handleClockTick(
            ReportStore reportStore,
            EventBus eventBus,
            CQLEngine cqlEngine,
            TicketManager ticketManager,
            MailSender mailSender,
            ExecutorService reportRunner,
            Map<String, String> currentlyRunningReports,
            Date date) {
        reportStore.listReports()
                .stream()
                .filter(report -> report.getState().equals(ReportState.ACTIVE))
                .forEach(report -> {
                    log.info("Scheduling report {} (ID: {}) for execution", report.getName(), report.getId());
                    reportRunner.submit(new ReportRunner(date,
                                                         report,
                                                         reportStore,
                                                         cqlEngine,
                                                         ticketManager,
                                                         mailSender,
                                                         eventBus,
                                                         currentlyRunningReports
                    ));
                });
    }

    @RequiredArgsConstructor
    private static class ReportRunner implements Runnable {
        private final SimpleDateFormat reportNameDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm");

        private final Date date;
        private final Report report;
        private final ReportStore reportStore;
        private final CQLEngine cqlEngine;
        private final TicketManager ticketManager;
        private final MailSender mailSender;
        private final EventBus eventBus;
        private final Map<String, String> currentlyRunningReports;

        @Override
        public void run() {
            try {
                val run = reportStore.relevantRuns(report.getId(), date, EnumSet.of(ReportRun.State.SCHEDULED), 1)
                        .stream()
                        .findFirst()
                        .orElse(null);
                if (null == run) {
                    log.debug("No candidate found for report {} that needs to be run right now", report.getId());
                    return;
                }
                checkAndRunReport(run,
                                  reportStore.readContext(report.getId())
                                          .orElseGet(() -> ReportContext.create(report.getId())));
            }
            catch (Throwable t) {
                log.error("Error executing report " + report.getId(), t);
            }

        }

        private void checkAndRunReport(ReportRun run, ReportContext context) {
            if (currentlyRunningReports.containsKey(report.getId())) {
                log.warn("Report run skipped as report processing is currently underway for report {} with runID {}",
                         report.getId(), run.getRunId());
                return;
            }
            currentlyRunningReports.put(run.getReportId(), run.getRunId());
            try {
                val result = runReport(run, context);
                reportStore.markCompleted(result.getReportId(),
                                          result.getRunId(),
                                          result,
                                          context);
                eventBus.publish(new ReportExecutionCompletedEvent(report.getId(), result));
            }
            catch (Exception e) {
                log.error("Error saving status for: " + run.getReportId() + "/" + run.getRunId(), e);
            }
            finally {
                currentlyRunningReports.remove(run.getReportId());
            }
        }

        @SneakyThrows
        private ReportRunResult runReport(ReportRun run, ReportContext context) {
            val reportId = report.getId();
            val meta = context.getData();
            var next = (String) meta.get("NEXT_POINTER");
            log.debug("Report run {}/{} starting with next pointer: {}", reportId, run.getRunId(), next);
            try {
                val parserOutput = cqlEngine.parse(report.getCqlQuery()).orElse(null);
                if (null == parserOutput) {
                    return new ReportRunResult(reportId,
                                               run.getRunId(),
                                               ReportRun.State.FAILED,
                                               "Parsing error for CQL: " + report.getCqlQuery());
                }
                var responseCount = 0;
                val file = File.createTempFile("conductor-report-" + run.getRunId(),
                                               reportNameDateFormat.format(date));
                var printer = (CSVPrinter) null;
                try (val writer = new FileWriter(file)) {
                    do {
                        val queryResponse = CQLEngine.runQuery(run.getRunId(),
                                                               next,
                                                               10,
                                                               parserOutput,
                                                               ticketManager);
                        val table = tabulate(queryResponse, parserOutput.selectedFields());
                        responseCount = getResponseCount(queryResponse);
                        next = nextPointer(queryResponse);
                        if (null == printer) {
                            printer = CSVFormat.Builder.create()
                                    .setHeader(table.columnKeySet().toArray(new String[0]))
                                    .build()
                                    .print(writer);
                        }
                        printer.printRecords(table.rowMap().values()
                                                     .stream()
                                                     .map(Map::values)
                                                     .toList());


                    }
                    while (responseCount > 0);
                }


                mailSender.send(new MailSender.Mail(report.getRecipients(),
                                                    "[CONDUCTOR REPORT] [" + report.getName()
                                                            + " (Date: " + reportNameDateFormat.format(date) + ")",
                                                    "Report attached.",
                                                    List.of(new MailSender.Attachment(file.getName(), file))));
                meta.put("NEXT_POINTER", next);
            }
            catch (Exception e) {
                log.error("Error running report: " + reportId + "/" + run.getRunId(), e);
                return new ReportRunResult(reportId,
                                           run.getRunId(),
                                           ReportRun.State.FAILED,
                                           "Error: " + e.getMessage());
            }
            return new ReportRunResult(reportId,
                                       run.getRunId(),
                                       ReportRun.State.FINISHED,
                                       "Sent Successfully");
        }

        private static Integer getResponseCount(TicketQueryResponse queryResponse) {
            return queryResponse.accept(new TicketQueryResponseVisitor<>() {
                @Override
                public Integer visit(TicketListResponse listResponse) {
                    return listResponse.getResults().size();
                }

                @Override
                public Integer visit(TicketGroupResponse groupResponse) {
                    return 0;
                }
            });
        }

        private static String nextPointer(TicketQueryResponse queryResponse) {
            return queryResponse.accept(new TicketQueryResponseVisitor<String>() {
                @Override
                public String visit(TicketListResponse listResponse) {
                    return listResponse.getNext();
                }

                @Override
                public String visit(TicketGroupResponse groupResponse) {
                    return null;
                }
            });
        }
    }


}
