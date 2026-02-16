package io.appform.conductor.core.actionmanagement.impl.models;

public interface StoredActionVisitor<T> {

    T visit(StoredSetFieldAction storedSetFieldAction);

    T visit(StoredAddCommentAction storedAddCommentAction);

    T visit(StoredAddTicketAction storedAddTicketAction);

    T visit(StoredChangePriorityAction storedChangePriorityAction);

    T visit(StoredRouteToGroupAction storedRouteToGroupAction);

    T visit(StoredWebhookAction storedWebhookAction);

}
