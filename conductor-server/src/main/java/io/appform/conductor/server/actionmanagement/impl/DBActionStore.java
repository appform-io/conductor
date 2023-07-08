package io.appform.conductor.server.actionmanagement.impl;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.actionmanagement.impl.models.StoredAction;
import io.appform.dropwizard.sharding.dao.LookupDao;
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

    private final LookupDao<StoredAction> actionLookupDao;


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAction.ACTION_TABLE_NAME))
    public Optional<Action> save(Action action) {
        return actionLookupDao.save(toDao(action))
                .map(this::toDto);
    }


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAction.ACTION_TABLE_NAME))
    public Optional<Action> read(@Throws.RuntimeParam("id") String actionId) {
        return actionLookupDao.get(actionId)
                .map(this::toDto);
    }


    private StoredAction toDao(Action action) {
        return null;
    }

    private Action toDto(StoredAction storedAction) {
        return null;
    }
}
