package io.appform.conductor.server.reporting;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.events.impl.reporting.ReportCreatedEvent;
import io.appform.conductor.model.events.impl.reporting.ReportDeletedEvent;
import io.appform.conductor.model.events.impl.reporting.ReportExecutionCompletedEvent;
import io.appform.conductor.model.events.impl.reporting.ReportStateUpdatedEvent;
import io.appform.conductor.model.reporting.Report;
import io.appform.conductor.model.reporting.ReportRun;
import io.appform.conductor.model.reporting.ReportRunResult;
import io.appform.conductor.model.reporting.ReportState;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Singleton
public class EventGeneratingReportStore implements ReportStore {
    private final EventBus eventBus;
    private final ReportStore reportStore;

    @Inject
    public EventGeneratingReportStore(EventBus eventBus, @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) ReportStore reportStore) {
        this.eventBus = eventBus;
        this.reportStore = reportStore;
    }

    @Override
    public Optional<Report> save(String id,
                                 String name,
                                 String description,
                                 String cql,
                                 List<String> emails,
                                 String cron,
                                 Scope scope) {
        val res = reportStore.save(id, name, description, cql, emails, cron, scope);
        res.ifPresent(report -> eventBus.publish(new ReportCreatedEvent(report.getId())));
        return res;
    }

    @Override
    public Optional<Report> updateState(String reportId, ReportState state) {
        val res = reportStore.updateState(reportId, state);
        res.ifPresent(report -> eventBus.publish(new ReportStateUpdatedEvent(report.getId(), report.getState())));
        return res;
    }

    @Override
    public Optional<Report> get(String id) {
        return reportStore.get(id);
    }

    @Override
    public boolean delete(String id) {
        val res = reportStore.delete(id);
        if(res) {
            eventBus.publish(new ReportDeletedEvent(id));
        }
        return res;
    }

    @Override
    public List<Report> listReports() {
        return reportStore.listReports();
    }

    @Override
    public List<ReportRun> runs(String reportId, Collection<ReportRun.State> states) {
        return reportStore.runs(reportId, states);
    }

    @Override
    public List<ReportRun> relevantRuns(String reportId, Date maxDate, Collection<ReportRun.State> states, int size) {
        return reportStore.relevantRuns(reportId, maxDate, states, size);
    }

    @Override
    public List<ReportRun> runsForStates(ReportRun.State state) {
        return reportStore.runsForStates(state);
    }

    @Override
    public void markCompleted(String reportId, String runId, ReportRunResult runResult, ReportContext updatedContext) {
        reportStore.markCompleted(reportId, runId, runResult, updatedContext);
        eventBus.publish(new ReportExecutionCompletedEvent(reportId, runResult));
    }

    @Override
    public Optional<ReportContext> readContext(String reportId) {
        return reportStore.readContext(reportId);
    }
}