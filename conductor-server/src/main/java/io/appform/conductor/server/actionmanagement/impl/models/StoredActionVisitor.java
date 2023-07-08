package io.appform.conductor.server.actionmanagement.impl.models;

public interface StoredActionVisitor<T> {

    T visit(StoredSetFieldAction storedSetFieldAction);

    T visit(StoredAddCommentAction storedAddCommentAction);

    T visit(StoredAddTicketAction storedAddTicketAction);

    T visit(StoredChangePriorityAction storedChangePriorityAction);

    T visit(StoredCompositionAction storedCompositionAction);

    T visit(StoredRouteToGroupAction storedRouteToGroupAction);

    T visit(StoredWebhookAction storedWebhookAction);

}
