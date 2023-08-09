/*
 * Copyright (c) 2023 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License"),
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.ticketmanagement.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.comments.Attachment;
import io.appform.conductor.model.ticket.comments.Comment;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFieldFilterVisitor;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.TicketFilterVisitor;
import io.appform.conductor.model.ticket.filter.fieldfilters.*;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.ticketmanagement.*;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredEmbeddedFieldValue;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.dropwizard.sharding.scroll.ScrollPointer;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;

import static io.appform.conductor.model.error.ConductorErrorCode.*;

/**
 * An RDBMS based implementation for {@link TicketStore}
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBTicketStore implements TicketStore {

    private final LookupDao<StoredTicketSkeleton> ticketDao;
    private final RelationalDao<StoredFieldValue> fieldDao;
    private final RelationalDao<StoredComment> commentDao;
    private final RelationalDao<StoredAttachment> attachmentDao;
    private final ObjectMapper mapper;

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public Optional<TicketSkeleton> create(
            @Throws.RuntimeParam("id") final String ticketId,
            final String title,
            final String description,
            final String workflowId,
            final String subjectId,
            final String ticketStateId,
            final TicketPriority priority,
            final List<TicketFieldData> fields) {
        ticketDao.saveAndGetExecutor(new StoredTicketSkeleton()
                                             .setTicketId(ticketId)
                                             .setTitle(title)
                                             .setDescription(description)
                                             .setWorkflowId(workflowId)
                                             .setCreatedByUserId(ConductorServerUtils.operatingUserId())
                                             .setSubjectId(subjectId)
                                             .setTicketStateId(ticketStateId)
                                             .setCreatedByUserId(ConductorServerUtils.operatingUserId())
                                             .setPriority(priority))
                .saveAll(fieldDao, ticket -> toStoredFields(ticket, fields))
                .execute();
        return read(ticketId, true);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public Optional<TicketSkeleton> read(@Throws.RuntimeParam("id") final String ticketId, boolean readFields) {
        return ticketDao.get(ticketId,
                             (UnaryOperator<Criteria>) criteria -> criteria.setFetchMode(StoredTicketSkeleton.Fields.fields,
                                                                                         FetchMode.JOIN))
                .map(ticket -> toSummary(ticket, true));
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public Optional<TicketSkeleton> update(
            @Throws.RuntimeParam("id") String ticketId,
            UnaryOperator<TicketSkeleton> updater,
            final List<TicketFieldData> fields) {
        ticketDao.lockAndGetExecutor(ticketId)
                .mutate(
                        ticket -> {
                            val updated = updater.apply(toSummary(ticket, false));
                            ticket.setTitle(updated.getTitle())
                                    .setDescription(updated.getDescription())
                                    .setSubjectId(updated.getSubjectId())
                                    .setTicketStateId(updated.getTicketStateId())
                                    .setPriority(updated.getPriority());
                        })
                .saveAll(fieldDao, ticket -> toStoredFields(ticket, fields))
                .execute();
        return read(ticketId, true);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public Optional<TicketSkeleton> update(
            @Throws.RuntimeParam("id") final String ticketId,
            final String title,
            final String description,
            final String subjectId,
            final String ticketStateId,
            final TicketPriority priority,
            final List<TicketFieldData> fields) {

        ticketDao.lockAndGetExecutor(ticketId)
                .mutate(ticket -> ticket.setTitle(title)
                        .setDescription(description)
                        .setSubjectId(subjectId)
                        .setTicketStateId(ticketStateId)
                        .setPriority(priority))
                .saveAll(fieldDao, ticket -> toStoredFields(ticket, fields))
                .execute();
        return read(ticketId, true);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public TicketSkeletonListResult list(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema) {
        val filterCriteria = DetachedCriteria.forClass(StoredTicketSkeleton.class, StoredFieldValue.Fields.ticket)
                .add(Property.forName(StoredTicketSkeleton.Fields.deleted).eq(false))
                .setProjection(Projections.distinct(Projections.property(StoredTicketSkeleton.Fields.ticketId)));
        applyTicketFilter(ticketFilters, filterCriteria);
        applyFieldFilters(fieldFilters, relevantFieldSchema, filterCriteria);
        val pointer = Strings.isNullOrEmpty(start)
                      ? null
                      : mapper.readValue(Base64.getUrlDecoder().decode(start.getBytes(StandardCharsets.UTF_8)),
                                         ScrollPointer.class);
        val resultCriteria = DetachedCriteria.forClass(StoredTicketSkeleton.class)
                .add(Property.forName(StoredTicketSkeleton.Fields.ticketId).in(filterCriteria));
        val results = ticketDao.scrollUp(resultCriteria, pointer, size, "id");
        return new TicketSkeletonListResult(
                results.getResult()
                        .stream()
                        .map(ticket -> toSummary(ticket, false))
                        .toList(),
                Base64.getUrlEncoder().encodeToString(
                        mapper.writeValueAsString(results.getPointer())
                                .getBytes(StandardCharsets.UTF_8))); //Keep charset consistent
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredComment.TICKET_COMMENTS_TABLE_NAME))
    public Optional<Comment> addComment(
            @Throws.RuntimeParam("id") String ticketId,
            @Throws.RuntimeParam("subId") String commentId,
            String comment,
            String inReplyTo) {
        return commentDao.save(ticketId,
                               new StoredComment()
                                       .setTicketId(ticketId)
                                       .setCommentId(commentId)
                                       .setAuthor(ConductorServerUtils.operatingUserId())
                                       .setContent(comment)
                                       .setReplyToId(inReplyTo))
                .map(DBTicketStore::toComment);
    }


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredComment.TICKET_COMMENTS_TABLE_NAME))
    public List<Comment> listComments(
            @Throws.RuntimeParam("id") String ticketId,
            int from,
            int size) {
        return listComments(ticketId,
                            DetachedCriteria.forClass(StoredComment.class)
                                    .add(Property.forName(StoredComment.Fields.ticketId).eq(ticketId))
                                    .add(Property.forName(StoredComment.Fields.deleted).eq(false)),
                            from,
                            size);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredComment.TICKET_COMMENTS_TABLE_NAME))
    public List<Comment> repliesToComment(
            @Throws.RuntimeParam("id") String ticketId,
            String replyToId,
            int from,
            int size) {
        return listComments(ticketId,
                            DetachedCriteria.forClass(StoredComment.class)
                                    .add(Property.forName(StoredComment.Fields.ticketId).eq(ticketId))
                                    .add(Property.forName(StoredComment.Fields.replyToId).eq(replyToId))
                                    .add(Property.forName(StoredComment.Fields.deleted).eq(false)),
                            from,
                            size);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttachment.TICKET_ATTACHMENTS_TABLE_NAME))
    public Optional<Attachment> registerAttachment(
            @Throws.RuntimeParam("id") String ticketId,
            @Throws.RuntimeParam("subId") String attachmentId,
            MediaType type,
            URL url,
            long sizeInBytes,
            boolean encrypted) {
        return attachmentDao.save(ticketId,
                                  new StoredAttachment()
                                          .setAttachmentId(attachmentId)
                                          .setTicketId(ticketId)
                                          .setCreator(ConductorServerUtils.operatingUserId())
                                          .setMediaType(type)
                                          .setUrl(url)
                                          .setEncrypted(encrypted)
                                          .setSizeInBytes(sizeInBytes))
                .map(DBTicketStore::toAttachment);
    }


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttachment.TICKET_ATTACHMENTS_TABLE_NAME))
    public List<Attachment> listAttachments(
            @Throws.RuntimeParam("id") String ticketId,
            int from,
            int size) {
        return attachmentDao.select(ticketId,
                                    DetachedCriteria.forClass(StoredAttachment.class)
                                            .add(Property.forName(StoredAttachment.Fields.ticketId).eq(ticketId))
                                            .add(Property.forName(StoredAttachment.Fields.deleted).eq(false)),
                                    from,
                                    size)
                .stream()
                .map(DBTicketStore::toAttachment)
                .toList();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAttachment.TICKET_ATTACHMENTS_TABLE_NAME))
    public boolean deleteAttachment(
            @Throws.RuntimeParam("id") String ticketId,
            @Throws.RuntimeParam("subId") String attachmentId) {
        return attachmentDao.update(ticketId,
                                    DetachedCriteria.forClass(StoredAttachment.class)
                                            .add(Property.forName(StoredAttachment.Fields.ticketId).eq(ticketId))
                                            .add(Property.forName(StoredAttachment.Fields.attachmentId)
                                                         .eq(attachmentId)),
                                    storedAttachment -> storedAttachment.setDeleted(true));
    }


    private void applyTicketFilter(List<TicketFilter> ticketFilters, DetachedCriteria criteria) {
        ticketFilters.forEach(filter -> filter.accept(new TicketFilterVisitor<Void>() {
            @Override
            public Void visit(TicketWorkflowEquals workflowEquals) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.workflowId)
                                     .eq(workflowEquals.getWorkflowId()));
                return null;
            }

            @Override
            public Void visit(TicketCreatedBy createdBy) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.createdByUserId)
                                     .eq(createdBy.getCreateByUserId()));
                return null;
            }

            @Override
            public Void visit(TicketAssignedToGroup assignedToGroup) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.assignedToGroupId)
                                     .eq(assignedToGroup.getAssignedGroupId()));
                return null;
            }

            @Override
            public Void visit(TicketUnAssignedToGroup unAssignedToGroup) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.assignedToGroupId).isNull());
                return null;
            }

            @Override
            public Void visit(TicketAssignedToUser assignedToUser) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.assignedToUserId)
                                     .eq(assignedToUser.getAssignedUserId()));
                return null;
            }

            @Override
            public Void visit(TicketUnAssignedToUser unAssignedToUser) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.assignedToUserId).isNull());
                return null;
            }

            @Override
            public Void visit(TicketSubjectEquals subjectEquals) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.subjectId).eq(subjectEquals.getSubjectId()));
                return null;
            }

            @Override
            public Void visit(TicketStateEquals stateEquals) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.ticketStateId).eq(stateEquals.getStateId()));
                return null;
            }

            @Override
            public Void visit(TicketStateIn stateIn) {
                if (stateIn.isNegate()) {
                    criteria.add(Restrictions.not(Restrictions.in(StoredTicketSkeleton.Fields.ticketStateId,
                                                                  stateIn.getStateIds())));
                }
                else {
                    criteria.add(Restrictions.in(StoredTicketSkeleton.Fields.ticketStateId, stateIn.getStateIds()));
                }

                return null;
            }

            @Override
            public Void visit(TicketPriorityEquals priorityEquals) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.priority).eq(priorityEquals.getPriority()));
                return null;
            }

            @Override
            public Void visit(TicketsCreatedTimeWindow createdTimeWindow) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.created)
                                     .gt(new Date(createdTimeWindow.getFrom()
                                                          .getTime() - createdTimeWindow.getDuration()
                                             .toMilliseconds())));
                return null;
            }

            @Override
            public Void visit(TicketsUpdatedTimeWindow updatedTimeWindow) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.updated)
                                     .gt(new Date(updatedTimeWindow.getFrom()
                                                          .getTime() - updatedTimeWindow.getDuration()
                                             .toMilliseconds())));
                return null;
            }
        }));
    }

    private static TicketSkeleton toSummary(final StoredTicketSkeleton skeleton, boolean readFields) {
        val fields = new ArrayList<StoredFieldValue>();
        if (readFields) {
            fields.addAll(Objects.requireNonNullElse(skeleton.getFields(), List.of()));
        }
        return new TicketSkeleton()
                .setTicketId(skeleton.getTicketId())
                .setTitle(skeleton.getTitle())
                .setDescription(skeleton.getDescription())
                .setWorkflowId(skeleton.getWorkflowId())
                .setCreatedByUserId(skeleton.getCreatedByUserId())
                .setAssignedToGroupId(skeleton.getAssignedToGroupId())
                .setAssignedToUserId(skeleton.getAssignedToUserId())
                .setTicketStateId(skeleton.getTicketStateId())
                .setSubjectId(skeleton.getSubjectId())
                .setPriority(skeleton.getPriority())
                .setDeleted(skeleton.isDeleted())
                .setCreated(skeleton.getCreated())
                .setUpdated(skeleton.getUpdated())
                .setFields(fields.stream()
                                   .map(DBTicketStore::toWireField)
                                   .toList());
    }

    private static List<StoredFieldValue> toStoredFields(StoredTicketSkeleton ticket, List<TicketFieldData> fields) {
        return Objects.requireNonNullElse(fields, List.<TicketFieldData>of())
                .stream()
                .map(field -> toStoredField(ticket, field))
                .toList();
    }

    private static TicketField toWireField(final StoredFieldValue value) {
        return new TicketField(value.getStoredEmbeddedFieldValue().getType(),
                               value.getSchemaFieldId(),
                               value.getStoredEmbeddedFieldValue().toFieldValue(),
                               value.getCreated(),
                               value.getUpdated());
    }

    private static StoredFieldValue toStoredField(
            final StoredTicketSkeleton ticket,
            final TicketFieldData data) {
        return new StoredFieldValue()
                .setFieldValueId(ticket.getTicketId() + "-" + data.getSchemaFieldId())
                .setStoredEmbeddedFieldValue(new StoredEmbeddedFieldValue(data.getValue()))
                .setTicket(ticket)
                .setSchemaFieldId(data.getSchemaFieldId());
    }

    private static void applyFieldFilters(
            List<TicketFieldFilter> filters,
            final Map<String, FieldSchema> relevantFields,
            DetachedCriteria rootCriteria) {
        var top = rootCriteria.createCriteria(StoredTicketSkeleton.Fields.fields, JoinType.LEFT_OUTER_JOIN);
        for (val filter : filters) {
            val fieldSchema = relevantFields.get(filter.getFieldSchemaId());
            val finalTop = top;
            augmentFilter(filter, fieldSchema, finalTop);
            top = top.createCriteria(StoredFieldValue.Fields.ticket)
                    .createCriteria(StoredTicketSkeleton.Fields.fields);

        }
    }

    @SuppressWarnings("java:S3776")
    private static void augmentFilter(TicketFieldFilter filter, FieldSchema fieldSchema, DetachedCriteria finalTop) {
        filter.accept(new TicketFieldFilterVisitor<Void>() {

            @Override
            public Void visit(TicketFieldEquals equals) {
                switch (fieldSchema.getType()) {
                    case STRING ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.stringValue).eq(equals.getValue()));
                    case BOOLEAN ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.booleanValue).eq(equals.getValue()));
                    case NUMBER ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).eq(equals.getValue()));
                    case DATE ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.dateValue).eq(equals.getValue()));
                    case CHOICE, LOCATION -> {
                        //NO OP
                    }
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldNotEquals notEquals) {
                switch (fieldSchema.getType()) {
                    case STRING -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.stringValue).ne(
                            notEquals.getValue()));
                    case BOOLEAN -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.booleanValue).ne(
                            notEquals.getValue()));
                    case NUMBER -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).ne(
                            notEquals.getValue()));
                    case DATE ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.dateValue).ne(notEquals.getValue()));
                    case CHOICE, LOCATION -> {
                        //NO OP
                    }
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldGreater greater) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).gt(greater.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldGreaterEquals greaterEquals) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).ge(greaterEquals.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldLesser lesser) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).lt(lesser.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldLesserEquals lesserEquals) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).le(lesserEquals.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldBetween between) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).between(between.getMin(),
                                                                                                      between.getMax()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldContainsChoices containsChoices) {
                if (fieldSchema.getType() == FieldType.CHOICE) {
                    val query = Restrictions.conjunction();
                    containsChoices.getChoices()
                            .forEach(choice -> query.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.choiceValue)
                                                                 .like(choice, MatchMode.ANYWHERE)));
                    finalTop.add(query);
                }
                return null;
            }

            private Property fieldConstraint(String actualName) {
                return Property.forName(String.join(".", StoredFieldValue.Fields.storedEmbeddedFieldValue, actualName));
            }
        });
    }

    private List<Comment> listComments(
            String ticketId,
            DetachedCriteria criteria,
            int from,
            int size) throws Exception {
        return commentDao.select(ticketId,
                                 criteria,
                                 from,
                                 size)
                .stream()
                .map(DBTicketStore::toComment)
                .toList();
    }

    private static Comment toComment(StoredComment storedComment) {
        return new Comment(storedComment.getCommentId(),
                           storedComment.getAuthor(),
                           storedComment.getContent(),
                           storedComment.getReplyToId(),
                           storedComment.isDeleted(),
                           storedComment.getCreated(),
                           storedComment.getUpdated());
    }

    private static Attachment toAttachment(StoredAttachment storedAttachment) {
        return new Attachment(storedAttachment.getAttachmentId(),
                              storedAttachment.getCreator(),
                              storedAttachment.getMediaType(),
                              storedAttachment.getUrl(),
                              storedAttachment.getSizeInBytes(),
                              storedAttachment.isEncrypted(),
                              storedAttachment.getCreated(),
                              storedAttachment.getUpdated());
    }
}


