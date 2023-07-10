package io.appform.conductor.server.actionmanagement.impl;

import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.ActionVisitor;
import io.appform.conductor.model.actions.impl.*;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.actionmanagement.impl.models.*;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_READ_ERROR;
import static io.appform.conductor.model.error.ConductorErrorCode.STORE_WRITE_ERROR;

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

    private StoredAction toStored(Action action) {
        return action.accept(new ActionVisitor<StoredAction>() {

            @Override
            public StoredAction visit(WebhookAction webhookAction) {
                return new StoredWebhookAction()
                        .setCallType(webhookAction.getCallType())
                        .setCallMode(webhookAction.getCallMode())
                        .setUrlTemplateType(webhookAction.getUrlTemplate().getType())
                        .setUrlTemplate(webhookAction.getUrlTemplate().getTemplate())
                        .setHeadersTemplateType(webhookAction.getHeadersTemplate().getType())
                        .setHeadersTemplate(webhookAction.getHeadersTemplate().getTemplate())
                        .setPayloadTemplateType(webhookAction.getPayloadTemplate().getType())
                        .setPayloadTemplate(webhookAction.getPayloadTemplate().getTemplate())
                        .setSuccessCodes(webhookAction.getSuccessCodes())
                        .setMimeType(webhookAction.getMimeType())
                        .setTimeoutMs(webhookAction.getTimeoutMs())
                        .setRetryStrategy(webhookAction.getRetryStrategy())
                        .setNumRetries(webhookAction.getNumRetries())
                        .setActionId(webhookAction.getId())
                        .setName(webhookAction.getName())
                        .setDescription(webhookAction.getDescription());
            }

            @Override
            public StoredAction visit(RouteToGroupAction routeToGroupAction) {
                return new StoredRouteToGroupAction()
                        .setGroupId(routeToGroupAction.getGroupId())
                        .setActionId(routeToGroupAction.getId())
                        .setName(routeToGroupAction.getName())
                        .setDescription(routeToGroupAction.getDescription());
            }

            @Override
            public StoredAction visit(AddCommentAction addCommentAction) {
                return new StoredAddCommentAction()
                        .setContentTemplateType(addCommentAction.getContentTemplate().getType())
                        .setContentTemplate(addCommentAction.getContentTemplate().getTemplate())
                        .setActionId(addCommentAction.getId())
                        .setName(addCommentAction.getName())
                        .setDescription(addCommentAction.getDescription());
            }

            @Override
            public StoredAction visit(AddTicketAction addTicketAction) {
                return new StoredAddTicketAction()
                        .setTicketActionId(addTicketAction.getActionId())
                        .setActionId(addTicketAction.getId())
                        .setName(addTicketAction.getName())
                        .setDescription(addTicketAction.getDescription());

            }

            @Override
            public StoredAction visit(ChangePriorityAction changePriorityAction) {
                return new StoredChangePriorityAction()
                        .setPriority(changePriorityAction.getPriority())
                        .setActionId(changePriorityAction.getId())
                        .setName(changePriorityAction.getName())
                        .setDescription(changePriorityAction.getDescription());
            }

            @Override
            public StoredAction visit(SetFieldAction setFieldAction) {
                return new StoredSetFieldAction()
                        .setFieldSchemaId(setFieldAction.getFieldSchemaId())
                        .setFieldValue(setFieldAction.getFieldValue())
                        .setActionId(setFieldAction.getId())
                        .setName(setFieldAction.getName())
                        .setDescription(setFieldAction.getDescription());

            }
        });
    }

    private Action toWired(StoredAction storedAction) {
        return storedAction.accept(new StoredActionVisitor<Action>() {
            @Override
            public Action visit(StoredSetFieldAction storedSetFieldAction) {
                return new SetFieldAction()
                        .setFieldSchemaId(storedSetFieldAction.getFieldSchemaId())
                        .setFieldValue(storedSetFieldAction.getFieldValue())
                        .setId(storedSetFieldAction.getActionId())
                        .setName(storedSetFieldAction.getName())
                        .setDescription(storedSetFieldAction.getDescription())
                        .setCreated(storedSetFieldAction.getCreated())
                        .setUpdated(storedSetFieldAction.getUpdated());
            }

            @Override
            public Action visit(StoredAddCommentAction storedAddCommentAction) {
                return new AddCommentAction()
                        .setContentTemplate(new Template(storedAddCommentAction.getContentTemplateType(),
                                storedAddCommentAction.getContentTemplate()))
                        .setId(storedAddCommentAction.getActionId())
                        .setName(storedAddCommentAction.getName())
                        .setDescription(storedAddCommentAction.getDescription())
                        .setCreated(storedAddCommentAction.getCreated())
                        .setUpdated(storedAddCommentAction.getUpdated());
            }

            @Override
            public Action visit(StoredAddTicketAction storedAddTicketAction) {
                return new AddTicketAction()
                        .setActionId(storedAddTicketAction.getTicketActionId())
                        .setId(storedAddTicketAction.getActionId())
                        .setName(storedAddTicketAction.getName())
                        .setDescription(storedAddTicketAction.getDescription())
                        .setCreated(storedAddTicketAction.getCreated())
                        .setUpdated(storedAddTicketAction.getUpdated());
            }

            @Override
            public Action visit(StoredChangePriorityAction storedChangePriorityAction) {
                return new ChangePriorityAction()
                        .setPriority(storedChangePriorityAction.getPriority())
                        .setId(storedChangePriorityAction.getActionId())
                        .setName(storedChangePriorityAction.getName())
                        .setDescription(storedChangePriorityAction.getDescription())
                        .setCreated(storedChangePriorityAction.getCreated())
                        .setUpdated(storedChangePriorityAction.getUpdated());
            }

            @Override
            public Action visit(StoredRouteToGroupAction storedRouteToGroupAction) {
                return new RouteToGroupAction()
                        .setGroupId(storedRouteToGroupAction.getGroupId())
                        .setId(storedRouteToGroupAction.getActionId())
                        .setName(storedRouteToGroupAction.getName())
                        .setDescription(storedRouteToGroupAction.getDescription())
                        .setCreated(storedRouteToGroupAction.getCreated())
                        .setUpdated(storedRouteToGroupAction.getUpdated());
            }

            @Override
            public Action visit(StoredWebhookAction storedWebhookAction) {
                return new WebhookAction()
                        .setCallType(storedWebhookAction.getCallType())
                        .setCallMode(storedWebhookAction.getCallMode())
                        .setUrlTemplate(new Template(storedWebhookAction.getUrlTemplateType(),
                                storedWebhookAction.getUrlTemplate()))
                        .setHeadersTemplate(new Template(storedWebhookAction.getHeadersTemplateType(),
                                storedWebhookAction.getHeadersTemplate()))
                        .setPayloadTemplate(new Template(storedWebhookAction.getPayloadTemplateType(),
                                storedWebhookAction.getPayloadTemplate()))
                        .setSuccessCodes(storedWebhookAction.getSuccessCodes())
                        .setMimeType(storedWebhookAction.getMimeType())
                        .setTimeoutMs(storedWebhookAction.getTimeoutMs())
                        .setRetryStrategy(storedWebhookAction.getRetryStrategy())
                        .setNumRetries(storedWebhookAction.getNumRetries())
                        .setId(storedWebhookAction.getActionId())
                        .setName(storedWebhookAction.getName())
                        .setDescription(storedWebhookAction.getDescription())
                        .setCreated(storedWebhookAction.getCreated())
                        .setUpdated(storedWebhookAction.getUpdated());
            }
        });
    }
}
