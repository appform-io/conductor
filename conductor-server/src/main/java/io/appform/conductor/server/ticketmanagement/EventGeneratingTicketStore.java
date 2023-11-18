package io.appform.conductor.server.ticketmanagement;

import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.TicketState;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.analytics.GroupingElement;
import io.appform.conductor.model.ticket.analytics.TicketGroupResponse;
import io.appform.conductor.model.ticket.comments.Attachment;
import io.appform.conductor.model.ticket.comments.Comment;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.*;
import lombok.NonNull;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Singleton
public class EventGeneratingTicketStore implements TicketStore {
    private final EventBus eventBus;
    private final TicketStore ticketStore;

    @Inject
    public EventGeneratingTicketStore(
            EventBus eventBus,
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) TicketStore ticketStore) {
        this.eventBus = eventBus;
        this.ticketStore = ticketStore;
    }

    @Override
    public Optional<TicketSkeleton> create(
            String ticketId,
            String title,
            String description,
            String workflowId,
            String subjectId,
            String ticketStateId,
            TicketPriority priority,
            List<TicketFieldData> fields) {
        val res = ticketStore.create(ticketId, title, description, workflowId,
                                     subjectId, ticketStateId, priority, fields);
        res.ifPresent(ticketSkeleton -> eventBus.publish(new TicketCreatedEvent(ticketSkeleton.getTicketId())));
        return res;
    }

    @Override
    public Optional<TicketSkeleton> read(String ticketId, boolean readFields) {
        return ticketStore.read(ticketId, readFields);
    }

    @Override
    public Optional<TicketSkeleton> update(
            String ticketId,
            UnaryOperator<TicketSkeleton> updater,
            List<TicketFieldData> fields) {
        //called by other update functions...this will raise duplicate events
        return ticketStore.update(ticketId, updater, fields);
    }

    @Override
    public Optional<TicketSkeleton> updateSkeleton(
            String ticketId,
            UnaryOperator<TicketSkeleton> updater) {
        val res = ticketStore.updateSkeleton(ticketId, updater);
        res.ifPresent(ticketSkeleton -> eventBus.publish(new TicketUpdatedEvent(ticketSkeleton.getTicketId())));
        return res;
    }

    @Override
    public Optional<TicketSkeleton> updateState(String ticketId, @NonNull TicketState newState) {
        val res = ticketStore.updateState(ticketId, newState);
        res.filter(ticketSkeleton -> ticketSkeleton.getTicketStateId().equals(newState.getId()))
                .ifPresent(ticketSkeleton -> eventBus.publish(new TicketStateUpdatedEvent(ticketSkeleton.getTicketId(),
                                                                                          ticketSkeleton.getTicketStateId())));
        return res;
    }

    @Override
    public Optional<TicketSkeleton> changePriority(String ticketId, @NonNull TicketPriority newPriority) {
        val res = ticketStore.changePriority(ticketId, newPriority);
        res.filter(ticketSkeleton -> ticketSkeleton.getPriority() == newPriority)
                .ifPresent(ticketSkeleton -> eventBus.publish(new TicketPriorityUpdatedEvent(ticketSkeleton.getTicketId(),
                                                                                             ticketSkeleton.getPriority())));
        return res;
    }

    @Override
    public Optional<TicketSkeleton> assignToGroup(String ticketId, @NonNull String groupId, String userId) {
        val res = ticketStore.assignToGroup(ticketId, groupId, userId);
        res.filter(ticketSkeleton -> !Strings.isNullOrEmpty(ticketSkeleton.getAssignedToGroupId()))
                .filter(ticketSkeleton -> ticketSkeleton.getAssignedToGroupId().equals(groupId))
                .ifPresent(ticketSkeleton -> eventBus.publish(new TicketGroupAssignedEvent(
                        ticketSkeleton.getTicketId(), groupId)));
        res.filter(ticketSkeleton -> !Strings.isNullOrEmpty(ticketSkeleton.getAssignedToUserId()))
                .filter(ticketSkeleton -> ticketSkeleton.getAssignedToUserId().equals(userId))
                .ifPresent(ticketSkeleton -> eventBus.publish(new TicketUserAssignedEvent(
                        ticketSkeleton.getTicketId(), userId)));
        return res;
    }


    @Override
    public Optional<TicketSkeleton> setFields(String ticketId, @NonNull List<TicketFieldData> fields) {
        val res = ticketStore.setFields(ticketId, fields);
        res.ifPresent(ticketSkeleton -> eventBus.publish(new TicketFieldsUpdatedEvent(ticketSkeleton.getTicketId(),
                                                                                      fields.stream()
                                                                                              .map(TicketFieldData::getSchemaFieldId)
                                                                                              .collect(Collectors.toList()))));
        return res;
    }

    @Override
    public Optional<TicketSkeleton> assignToUser(String ticketId, @NonNull String userId) {
        val res = ticketStore.assignToUser(ticketId, userId);
        res.filter(ticketSkeleton -> ticketSkeleton.getAssignedToUserId().equals(userId))
                .ifPresent(ticketSkeleton -> eventBus.publish(new TicketUserAssignedEvent(
                        ticketSkeleton.getTicketId(), userId)));
        return res;
    }

    @Override
    public Optional<TicketSkeleton> update(
            String ticketId,
            String title,
            String description,
            String subjectId,
            String ticketStateId,
            TicketPriority priority,
            List<TicketFieldData> fields) {
        val res = ticketStore.update(ticketId, title, description, subjectId,
                                     ticketStateId, priority, fields);
        res.ifPresent(ticketSkeleton -> eventBus.publish(new TicketUpdatedEvent(ticketSkeleton.getTicketId())));
        return res;
    }

    @Override
    public TicketSkeletonListResult older(
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            String start,
            int size,
            Map<String, FieldSchema> relevantFieldSchema,
            boolean readFields, List<String> fieldNames) {
        return ticketStore.older(ticketFilters, fieldFilters, start, size, relevantFieldSchema, readFields, fieldNames);
    }

    @Override
    public TicketSkeletonListResult since(
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            String start,
            int size,
            Map<String, FieldSchema> relevantFieldSchema,
            boolean readFields, List<String> fieldsToBeFetched) {
        return ticketStore.since(ticketFilters, fieldFilters, start, size, relevantFieldSchema,
                                 readFields,
                                 fieldsToBeFetched);
    }

    @Override
    public TicketGroupResponse groupCount(
            String requestId,
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema,
            List<GroupingElement> groupingElements) {
        return ticketStore.groupCount(requestId,
                                      ticketFilters,
                                      fieldFilters,
                                      relevantFieldSchema,
                                      groupingElements);
    }

    @Override
    public Optional<Comment> addComment(String ticketId, String commentId, String comment, String inReplyTo) {
        val res = ticketStore.addComment(ticketId, commentId, comment, inReplyTo);
        res.ifPresent(commentAdded -> eventBus.publish(new CommentAddedEvent(ticketId, commentAdded.getId())));
        return res;
    }

    @Override
    public List<Comment> listComments(String ticketId, int from, int size) {
        return ticketStore.listComments(ticketId, from, size);
    }

    @Override
    public List<Comment> repliesToComment(String ticketId, String replyToId, int from, int size) {
        return ticketStore.repliesToComment(ticketId, replyToId, from, size);
    }

    @Override
    public Optional<Attachment> registerAttachment(
            String ticketId,
            String attachmentId,
            MediaType type,
            URL url,
            long sizeInBytes,
            boolean encrypted) {
        val res = ticketStore.registerAttachment(ticketId, attachmentId, type, url, sizeInBytes, encrypted);
        res.ifPresent(attachment -> eventBus.publish(new AttachmentAddedEvent(ticketId, attachment.getId())));
        return res;
    }

    @Override
    public List<Attachment> listAttachments(String ticketId, int from, int size) {
        return ticketStore.listAttachments(ticketId, from, size);
    }

    @Override
    public boolean deleteAttachment(String ticketId, String attachmentId) {
        val res = ticketStore.deleteAttachment(ticketId, attachmentId);
        if (res) {
            eventBus.publish(new AttachmentDeletedEvent(ticketId, attachmentId));
        }
        return res;
    }

    @Override
    public Optional<RelatedTicket> addRelatedTicket(String ticketId, String relatedToTicketId, TicketRelationship relationship) {
        val res = ticketStore.addRelatedTicket(ticketId, relatedToTicketId, relationship);
        res.ifPresent(attachment -> eventBus.publish(new RelatedTicketAddedEvent(ticketId, relatedToTicketId, relationship)));
        return res;
    }

    @Override
    public List<RelatedTicket> listRelatedTickets(String ticketId, int from, int size) {
        return ticketStore.listRelatedTickets(ticketId, from, size);
    }

    @Override
    public boolean deleteRelatedTicket(String ticketId, String relatedToTicketId) {
        val res =  ticketStore.deleteRelatedTicket(ticketId, relatedToTicketId);
        if (res) {
            eventBus.publish(new RelatedTicketDeletedEvent(ticketId, relatedToTicketId));
        }
        return res;
    }
}
