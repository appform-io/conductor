package io.appform.conductor.server.actionmanagement.impl;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.ActionVisitor;
import io.appform.conductor.model.actions.impl.*;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.actionmanagement.impl.models.*;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredEmbeddedFieldValue;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_READ_ERROR;
import static io.appform.conductor.model.error.ConductorErrorCode.STORE_WRITE_ERROR;
import static java.util.stream.Collectors.toList;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBActionStore implements ActionStore {

    private final LookupDao<StoredAction> actionDao;


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAction.ACTION_TABLE_NAME))
    public Optional<Action> save(Action action) {
        return actionDao.save(toStored(action))
                .map(this::toWired);
    }


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAction.ACTION_TABLE_NAME))
    public Optional<Action> read(@Throws.RuntimeParam("id") String actionId) {
        return actionDao.get(actionId)
                .map(this::toWired);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAction.ACTION_TABLE_NAME))
    public List<Action> read(@Throws.RuntimeParam("id") List<String> actions) {
        return actionDao.get(actions)
                .stream()
                .map(this::toWired)
                .collect(Collectors.toList());
    }

    private StoredAction toStored(Action action) {
        val storedAction =   action.accept(new ActionVisitor<StoredAction>() {

            @Override
            public StoredAction visit(WebhookAction webhookAction) {
                val storedWebhookAction =  new StoredWebhookAction()
                        .setCallType(webhookAction.getCallType())
                        .setCallMode(webhookAction.getCallMode())
                        .setUrlTemplate(webhookAction.getUrlTemplate())
                        .setPayloadTemplate(webhookAction.getPayloadTemplate())
                        .setSuccessCodes(webhookAction.getSuccessCodes())
                        .setMimeType(webhookAction.getMimeType())
                        .setTimeoutMs(webhookAction.getTimeoutMs())
                        .setRetryStrategy(webhookAction.getRetryStrategy())
                        .setNumRetries(webhookAction.getNumRetries());

                storedWebhookAction.setHeaderTemplates(webhookAction.getHeaderTemplates()
                        .entrySet()
                        .stream()
                        .map(header ->
                                new StoredWebhookActionHeaderTemplate()
                                        .setActive(true)
                                        .setAction(storedWebhookAction)
                                        .setName(header.getKey())
                                        .setTemplate(header.getValue())
                        ).collect(Collectors.toList()));
                return storedWebhookAction;
            }

            @Override
            public StoredAction visit(RouteToGroupAction routeToGroupAction) {
                return new StoredRouteToGroupAction()
                        .setGroupId(routeToGroupAction.getGroupId());
            }

            @Override
            public StoredAction visit(AddCommentAction addCommentAction) {
                return new StoredAddCommentAction()
                        .setContentTemplate(addCommentAction.getContentTemplate());
            }

            @Override
            public StoredAction visit(AddTicketAction addTicketAction) {
                return new StoredAddTicketAction()
                        .setTicketActionId(addTicketAction.getActionId());

            }

            @Override
            public StoredAction visit(ChangePriorityAction changePriorityAction) {
                return new StoredChangePriorityAction()
                        .setPriority(changePriorityAction.getPriority());
            }

            @Override
            public StoredAction visit(SetFieldAction setFieldAction) {
                return new StoredSetFieldAction()
                        .setFieldSchemaId(setFieldAction.getFieldSchemaId())
                        .setStoredFieldValue(new StoredEmbeddedFieldValue(setFieldAction.getFieldValue()));

            }
        });

        storedAction.setActionId(action.getId())
                .setName(action.getName())
                .setDescription(action.getDescription());

        return storedAction;
    }

    private Action toWired(StoredAction storedAction) {
        return storedAction.accept(new StoredActionVisitor<Action>() {
            @Override
            public Action visit(StoredSetFieldAction storedSetFieldAction) {
                return SetFieldAction.builder()
                        .id(storedSetFieldAction.getActionId())
                        .name(storedSetFieldAction.getName())
                        .description(storedSetFieldAction.getDescription())
                        .fieldSchemaId(storedSetFieldAction.getFieldSchemaId())
                        .fieldValue(storedSetFieldAction.getStoredFieldValue().toFieldValue())
                        .created(storedSetFieldAction.getCreated())
                        .updated(storedSetFieldAction.getUpdated())
                        .build();
            }

            @Override
            public Action visit(StoredAddCommentAction storedAddCommentAction) {
                return AddCommentAction.builder()
                        .id(storedAddCommentAction.getActionId())
                        .name(storedAddCommentAction.getName())
                        .description(storedAddCommentAction.getDescription())
                        .contentTemplate(storedAddCommentAction.getContentTemplate())
                        .created(storedAddCommentAction.getCreated())
                        .updated(storedAddCommentAction.getUpdated())
                        .build();
            }

            @Override
            public Action visit(StoredAddTicketAction storedAddTicketAction) {
                return AddTicketAction.builder()
                        .id(storedAddTicketAction.getActionId())
                        .name(storedAddTicketAction.getName())
                        .description(storedAddTicketAction.getDescription())
                        .actionId(storedAddTicketAction.getTicketActionId())
                        .created(storedAddTicketAction.getCreated())
                        .updated(storedAddTicketAction.getUpdated())
                        .build();
            }

            @Override
            public Action visit(StoredChangePriorityAction storedChangePriorityAction) {
                return ChangePriorityAction.builder()
                        .id(storedChangePriorityAction.getActionId())
                        .name(storedChangePriorityAction.getName())
                        .description(storedChangePriorityAction.getDescription())
                        .priority(storedChangePriorityAction.getPriority())
                        .created(storedChangePriorityAction.getCreated())
                        .updated(storedChangePriorityAction.getUpdated())
                        .build();
            }

            @Override
            public Action visit(StoredRouteToGroupAction storedRouteToGroupAction) {
                return RouteToGroupAction.builder()
                        .id(storedRouteToGroupAction.getActionId())
                        .name(storedRouteToGroupAction.getName())
                        .description(storedRouteToGroupAction.getDescription())
                        .groupId(storedRouteToGroupAction.getGroupId())
                        .created(storedRouteToGroupAction.getCreated())
                        .updated(storedRouteToGroupAction.getUpdated())
                        .build();
            }

            @Override
            public Action visit(StoredWebhookAction storedWebhookAction) {
                return WebhookAction.builder()
                        .id(storedWebhookAction.getActionId())
                        .name(storedWebhookAction.getName())
                        .description(storedWebhookAction.getDescription())
                        .callType(storedWebhookAction.getCallType())
                        .callMode(storedWebhookAction.getCallMode())
                        .urlTemplate(storedWebhookAction.getUrlTemplate())
                        .headerTemplates(storedWebhookAction.getHeaderTemplates()
                                .stream()
                                .filter(StoredWebhookActionHeaderTemplate::isActive)
                                .collect(Collectors.toMap(StoredWebhookActionHeaderTemplate::getName,
                                        StoredWebhookActionHeaderTemplate::getTemplate)))
                        .payloadTemplate(storedWebhookAction.getPayloadTemplate())
                        .successCodes(storedWebhookAction.getSuccessCodes())
                        .mimeType(storedWebhookAction.getMimeType())
                        .timeoutMs(storedWebhookAction.getTimeoutMs())
                        .retryStrategy(storedWebhookAction.getRetryStrategy())
                        .numRetries(storedWebhookAction.getNumRetries())
                        .id(storedWebhookAction.getActionId())
                        .name(storedWebhookAction.getName())
                        .description(storedWebhookAction.getDescription())
                        .created(storedWebhookAction.getCreated())
                        .updated(storedWebhookAction.getUpdated())
                        .build();
            }
        });
    }
}
