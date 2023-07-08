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
import java.util.stream.Collectors;

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
        return action.accept(new ActionVisitor<StoredAction>() {
            @Override
            public StoredAction visit(CompositionAction compositionAction) {
                final StoredCompositionAction storedCompositionAction =  StoredCompositionAction.builder()
                        .actionId(compositionAction.getId())
                        .name(compositionAction.getName())
                        .description(compositionAction.getDescription())
                        .build();

                storedCompositionAction.setChildren(
                        compositionAction.getChildren().stream()
                                .map(child -> {
                                    StoredAction storedChild = toDao(child);
                                    storedChild.setParentAction(storedCompositionAction);
                                    return storedChild;
                                }).collect(Collectors.toList()));

                return storedCompositionAction;
            }

            @Override
            public StoredAction visit(WebhookAction webhookAction) {
                return StoredWebhookAction.builder()
                        .actionId(webhookAction.getId())
                        .name(webhookAction.getName())
                        .description(webhookAction.getDescription())
                        .callType(webhookAction.getCallType())
                        .callMode(webhookAction.getCallMode())
                        .urlTemplate(webhookAction.getUrlTemplate())
                        .headersTemplate(webhookAction.getHeadersTemplate())
                        .payloadTemplate(webhookAction.getPayloadTemplate())
                        .successCodes(webhookAction.getSuccessCodes())
                        .mimeType(webhookAction.getMimeType())
                        .timeoutMs(webhookAction.getTimeoutMs())
                        .retryStrategy(webhookAction.getRetryStrategy())
                        .numRetries(webhookAction.getNumRetries())
                        .build();
            }

            @Override
            public StoredAction visit(RouteToGroupAction routeToGroupAction) {
                return StoredRouteToGroupAction.builder()
                        .actionId(routeToGroupAction.getId())
                        .name(routeToGroupAction.getName())
                        .description(routeToGroupAction.getDescription())
                        .groupId(routeToGroupAction.getGroupId())
                        .build();
            }

            @Override
            public StoredAction visit(AddCommentAction addCommentAction) {
                return StoredAddCommentAction.builder()
                        .actionId(addCommentAction.getId())
                        .name(addCommentAction.getName())
                        .description(addCommentAction.getDescription())
                        .contentTemplate(addCommentAction.getContentTemplate())
                        .build();
            }

            @Override
            public StoredAction visit(AddTicketAction addTicketAction) {
                return StoredAddTicketAction.builder()
                        .actionId(addTicketAction.getId())
                        .name(addTicketAction.getName())
                        .description(addTicketAction.getDescription())
                        .ticketActionId(addTicketAction.getActionId())
                        .build();
            }

            @Override
            public StoredAction visit(ChangePriorityAction changePriorityAction) {
                return StoredChangePriorityAction.builder()
                        .actionId(changePriorityAction.getId())
                        .name(changePriorityAction.getName())
                        .description(changePriorityAction.getDescription())
                        .priority(changePriorityAction.getPriority())
                        .build();
            }

            @Override
            public StoredAction visit(SetFieldAction setFieldAction) {
                return StoredSetFieldAction.builder()
                        .actionId(setFieldAction.getId())
                        .name(setFieldAction.getName())
                        .description(setFieldAction.getDescription())
                        .fieldSchemaId(setFieldAction.getFieldSchemaId())
                        .fieldValue(setFieldAction.getFieldValue())
                        .build();
            }
        });
    }

    private Action toDto(StoredAction storedAction) {
        return storedAction.accept(new StoredActionVisitor<Action>() {
            @Override
            public Action visit(StoredSetFieldAction storedSetFieldAction) {
                return SetFieldAction.builder()
                        .id(storedSetFieldAction.getActionId())
                        .name(storedSetFieldAction.getName())
                        .description(storedSetFieldAction.getDescription())
                        .fieldSchemaId(storedSetFieldAction.getFieldSchemaId())
                        .fieldValue(storedSetFieldAction.getFieldValue())
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
                        .contentTemplate(new Template(storedAddCommentAction.getContentTemplateType(),
                                storedAddCommentAction.getContentTemplate()))
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
            public Action visit(StoredCompositionAction storedCompositionAction) {
                return CompositionAction.builder()
                        .id(storedCompositionAction.getActionId())
                        .name(storedCompositionAction.getName())
                        .description(storedCompositionAction.getDescription())
                        .errorHandlingStrategy(storedCompositionAction.getActionErrorHandlingStrategy())
                        .children(storedCompositionAction.getChildren().stream()
                                .map(child -> toDto(child))
                                .collect(Collectors.toList()))
                        .created(storedCompositionAction.getCreated())
                        .updated(storedCompositionAction.getUpdated())
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
                        .urlTemplate(new Template(storedWebhookAction.getUrlTemplateType(),
                                storedWebhookAction.getUrlTemplate()))
                        .headersTemplate(new Template(storedWebhookAction.getHeadersTemplateType(),
                                storedWebhookAction.getHeadersTemplate()))
                        .payloadTemplate(new Template(storedWebhookAction.getPayloadTemplateType(),
                                storedWebhookAction.getPayloadTemplate()))
                        .successCodes(storedWebhookAction.getSuccessCodes())
                        .mimeType(storedWebhookAction.getMimeType())
                        .timeoutMs(storedWebhookAction.getTimeoutMs())
                        .retryStrategy(storedWebhookAction.getRetryStrategy())
                        .numRetries(storedWebhookAction.getNumRetries())
                        .created(storedWebhookAction.getCreated())
                        .updated(storedWebhookAction.getUpdated())
                        .build();
            }
        });
    }
}
