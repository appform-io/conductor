package io.appform.conductor.server.actionmanagement.impl;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_WRITE_ERROR;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBActionStore implements ActionStore {

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public Optional<Action> read(String actionId) {
        return Optional.empty();
    }
}
