package io.appform.conductor.server.actionmanagement.impl;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import io.appform.conductor.model.actions.Action;
import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.actions.ActionVisitor;
import io.appform.conductor.model.actions.impl.*;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.actionmanagement.ActionStore;
import io.appform.conductor.server.actionmanagement.impl.models.*;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredEmbeddedFieldValue;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
        return actionDao.save(toStored(action, null))
                .map(this::toWired);
    }

    @Override
    public boolean update(String actionId, UnaryOperator<Action> handler) {
        return actionDao.update(actionId,
                                storedAction -> storedAction.map(s -> toStored(handler.apply(toWired(s)), s))
                                        .orElse(null));
    }

    @Override
    public List<Action> listActionsForScopes(final Collection<Scope> scopes) {
        val criteria = DetachedCriteria.forClass(StoredAction.class)
                .add(Property.forName(StoredAction.Fields.deleted).eq(false));
        val scopeChain = Restrictions.or();
        scopes.forEach(scope -> scopeChain.add(Restrictions.and(
                Property.forName(StoredAction.Fields.scopeType).eq(scope.getType()),
                Property.forName(StoredAction.Fields.scopeReferenceId).eq(scope.getReferenceId()))));
        return actionDao.scatterGather(criteria.add(scopeChain))
                .stream()
                .map(this::toWired)
                .toList();
    }

    @Override
    public List<Action> listActionsForIds(final Collection<String> actionIds) {
        if(null == actionIds || actionIds.isEmpty()) {
            return List.of();
        }
        val criteria = DetachedCriteria.forClass(StoredAction.class)
                .add(Property.forName(StoredAction.Fields.deleted).eq(false))
                .add(Property.forName(StoredAction.Fields.actionId).in(actionIds));
        return actionDao.scatterGather(criteria)
                .stream()
                .map(this::toWired)
                .toList();
    }

    @Override
    public boolean delete(String actionId) {
        return actionDao.update(actionId,
                                storedAction -> storedAction.map(action -> action.setDeleted(true)).orElse(null));
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAction.ACTION_TABLE_NAME))
    public Optional<Action> read(@Throws.RuntimeParam("id") String actionId) {
        return actionDao.get(actionId)
                .filter(storedAction -> !storedAction.isDeleted())
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

    private StoredAction toStored(Action action, StoredAction existing) {
        val storedAction = action.accept(new ActionVisitor<StoredAction>() {

            @Override
            public StoredAction visit(WebhookAction webhookAction) {
                val storedWebhookAction = safeCast(existing, new StoredWebhookAction())
                        .setCallType(webhookAction.getCallType())
                        .setCallMode(webhookAction.getCallMode())
                        .setUrlTemplate(webhookAction.getUrlTemplate())
                        .setPayloadTemplate(webhookAction.getPayloadTemplate())
                        .setSuccessCodes(webhookAction.getSuccessCodes())
                        .setMimeType(webhookAction.getMimeType())
                        .setTimeoutMs(webhookAction.getTimeoutMs())
                        .setRetryStrategy(webhookAction.getRetryStrategy())
                        .setNumRetries(webhookAction.getNumRetries());
                val existingHeaders = new HashMap<>(Objects.requireNonNullElse(storedWebhookAction.getHeaderTemplates(),
                                                                 List.<StoredWebhookActionHeaderTemplate>of())
                        .stream()
                        .collect(Collectors.toMap(StoredWebhookActionHeaderTemplate::getName, Function.identity())));
                val newHeaders = Objects.requireNonNullElse(webhookAction.getHeaderTemplates(),
                                                            Map.<String, Template>of());
                //Delete non existing
                Sets.difference(existingHeaders.keySet(), newHeaders.keySet())
                                .forEach(missingHeader -> existingHeaders.get(missingHeader).setActive(false));
                //Now create or update
                newHeaders.forEach((header, value) -> {
                    existingHeaders.compute(header, (headerName, existing) -> {
                        if(null != existing) {
                            return existing.setTemplate(value).setActive(true);
                        }
                        return new StoredWebhookActionHeaderTemplate()
                                .setId(action.getId() + "-" + ConductorServerUtils.lowerSnake(headerName))
                                .setActive(true)
                                .setAction(storedWebhookAction)
                                .setName(headerName)
                                .setTemplate(value);
                    });
                });
                return storedWebhookAction.setHeaderTemplates(List.copyOf(existingHeaders.values()));
            }

            @Override
            public StoredAction visit(RouteToGroupAction routeToGroupAction) {
                return safeCast(existing, new StoredRouteToGroupAction())
                        .setGroupId(routeToGroupAction.getGroupId());
            }

            @Override
            public StoredAction visit(AddCommentAction addCommentAction) {
                return safeCast(existing, new StoredAddCommentAction())
                        .setContentTemplate(addCommentAction.getContentTemplate());
            }

            @Override
            public StoredAction visit(AddTicketAction addTicketAction) {
                return safeCast(existing, new StoredAddTicketAction())
                        .setTicketActionId(addTicketAction.getActionId());

            }

            @Override
            public StoredAction visit(ChangePriorityAction changePriorityAction) {
                return safeCast(existing, new StoredChangePriorityAction())
                        .setPriority(changePriorityAction.getPriority());
            }

            @Override
            public StoredAction visit(SetFieldAction setFieldAction) {
                return safeCast(existing, new StoredSetFieldAction())
                        .setFieldSchemaId(setFieldAction.getFieldSchemaId())
                        .setStoredFieldValue(new StoredEmbeddedFieldValue(setFieldAction.getFieldValue()));

            }
        });
        if (null == existing) {
            if (action.getScope() == null) {
                storedAction.setScopeType(Scope.ScopeType.GLOBAL)
                        .setScopeReferenceId(Scope.GLOBAL_STATE_REF_ID);
            }
            else {
                storedAction.setScopeType(action.getScope().getType())
                        .setScopeReferenceId(action.getScope().getReferenceId());
            }
            storedAction.setActionId(action.getId());
        }
        Objects.requireNonNullElse(existing, storedAction)
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
                        .scope(Scope.build(storedSetFieldAction.getScopeType(),
                                           storedSetFieldAction.getScopeReferenceId()))
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
                        .scope(Scope.build(storedAddCommentAction.getScopeType(),
                                           storedAddCommentAction.getScopeReferenceId()))
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
                        .scope(Scope.build(storedAddTicketAction.getScopeType(),
                                           storedAddTicketAction.getScopeReferenceId()))
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
                        .scope(Scope.build(storedChangePriorityAction.getScopeType(),
                                           storedChangePriorityAction.getScopeReferenceId()))
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
                        .scope(Scope.build(storedRouteToGroupAction.getScopeType(),
                                           storedRouteToGroupAction.getScopeReferenceId()))
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
                        .scope(Scope.build(storedWebhookAction.getScopeType(),
                                           storedWebhookAction.getScopeReferenceId()))
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

    private static abstract class Creator<T> {
        TypeToken<T> type = new TypeToken<>() {
        };

        abstract T create();
    }

    public static <T extends StoredAction> T safeCast(StoredAction action, T newObject) {

        val type = TypeToken.of(newObject.getClass());

        return Objects.requireNonNullElse(action, newObject)
                .accept(new StoredActionVisitor<>() {
                    @Override
                    public T visit(StoredSetFieldAction storedSetFieldAction) {
                        return type.getRawType().equals(storedSetFieldAction.getClass()) ? (T) storedSetFieldAction
                                                                                         : null;
                    }

                    @Override
                    public T visit(StoredAddCommentAction storedAddCommentAction) {
                        return type.getRawType().equals(storedAddCommentAction.getClass()) ? (T) storedAddCommentAction
                                                                                           : null;
                    }

                    @Override
                    public T visit(StoredAddTicketAction storedAddTicketAction) {
                        return type.getRawType().equals(storedAddTicketAction.getClass()) ? (T) storedAddTicketAction
                                                                                          : null;
                    }

                    @Override
                    public T visit(StoredChangePriorityAction storedChangePriorityAction) {
                        return type.getRawType().equals(storedChangePriorityAction.getClass())
                               ? (T) storedChangePriorityAction : null;
                    }

                    @Override
                    public T visit(StoredRouteToGroupAction storedRouteToGroupAction) {
                        return type.getRawType().equals(storedRouteToGroupAction.getClass())
                               ? (T) storedRouteToGroupAction : null;
                    }

                    @Override
                    public T visit(StoredWebhookAction storedWebhookAction) {
                        return type.getRawType().equals(storedWebhookAction.getClass()) ? (T) storedWebhookAction
                                                                                        : null;
                    }
                });

    }
}
