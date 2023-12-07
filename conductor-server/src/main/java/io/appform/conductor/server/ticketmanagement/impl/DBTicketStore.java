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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.ExternalReferenceID;
import io.appform.conductor.model.ticket.TicketRelationship;
import io.appform.conductor.model.ticket.analytics.*;
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
import io.appform.conductor.server.ticketmanagement.impl.models.StoredRelatedTicket;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredEmbeddedFieldValue;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.Pair;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static io.appform.conductor.model.error.ConductorErrorCode.*;
import static org.hibernate.criterion.CriteriaSpecification.DISTINCT_ROOT_ENTITY;

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
    private final RelationalDao<StoredRelatedTicket> relatedTicketDao;
    private final ObjectMapper mapper;


    private record TicketScrollPointer(Map<Integer, Integer> pointPerShard) implements Serializable {
        @Serial
        private static final long serialVersionUID = 8841802654321192764L;


        public void advance(int shard, int advanceBy) {
            pointPerShard.compute(shard, (key, existing) -> (null == existing ? 0 : existing) + advanceBy);
        }

        public int getCurrOffset(int shard) {
            return pointPerShard.computeIfAbsent(shard, key -> 0);
        }

        public static TicketScrollPointer deserializePointer(String start, ObjectMapper mapper) throws IOException {
            return Strings.isNullOrEmpty(start)
                   ? new TicketScrollPointer(new ConcurrentHashMap<>())
                   : mapper.readValue(Base64.getUrlDecoder().decode(start.getBytes(StandardCharsets.UTF_8)),
                                      TicketScrollPointer.class);
        }

        public static String serializePointer(
                TicketScrollPointer pointer,
                ObjectMapper mapper) throws JsonProcessingException {
            return Base64.getUrlEncoder().encodeToString(
                    mapper.writeValueAsString(pointer).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SKELETON_TABLE_NAME))
    public Optional<TicketSkeleton> create(
            @Throws.RuntimeParam("id") final String ticketId,
            final String title,
            final String description,
            final String workflowId,
            final String subjectId,
            final String ticketStateId,
            final TicketPriority priority,
            final ExternalReferenceID externalReferenceID,
            final List<TicketFieldData> fields) {
        ticketDao.saveAndGetExecutor(new StoredTicketSkeleton()
                                             .setTicketId(ticketId)
                                             .setTitle(title)
                                             .setDescription(description)
                                             .setWorkflowId(workflowId)
                                             .setCreatedByUserId(ConductorServerUtils.operatingUserId())
                                             .setSubjectId(subjectId)
                                             .setTicketStateId(ticketStateId)
                                             .setExternalReferenceId(externalReferenceId(externalReferenceID))
                                             .setExternalReferenceSource(externalReferenceSource(externalReferenceID))
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
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SKELETON_TABLE_NAME))
    public Optional<TicketSkeleton> read(@Throws.RuntimeParam("id") final String ticketId, boolean readFields) {
        return ticketDao.get(ticketId,
                             (UnaryOperator<Criteria>) criteria -> criteria.setFetchMode(StoredTicketSkeleton.Fields.fields,
                                                                                         FetchMode.JOIN))
                .map(ticket -> toSummary(ticket, true, List.of()));
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SKELETON_TABLE_NAME))
    public boolean ticketExists(final String workflowId) {
        DetachedCriteria criteria = DetachedCriteria.forClass(StoredTicketSkeleton.class)
                .add(Property.forName(StoredTicketSkeleton.Fields.workflowId).eq(workflowId));
        return !ticketDao.scrollDown(criteria, null, 1, StoredTicketSkeleton.Fields.ticketId)
                .getResult().isEmpty();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SKELETON_TABLE_NAME))
    public Optional<TicketSkeleton> update(
            @Throws.RuntimeParam("id") String ticketId,
            UnaryOperator<TicketSkeleton> updater,
            final List<TicketFieldData> fields) {
        ticketDao.lockAndGetExecutor(ticketId)
                .mutate(
                        ticket -> {
                            val updated = updater.apply(toSummary(ticket, false, List.of()));
                            ticket.setTitle(updated.getTitle())
                                    .setDescription(updated.getDescription())
                                    .setAssignedToGroupId(updated.getAssignedToGroupId())
                                    .setAssignedToUserId(updated.getAssignedToUserId())
                                    .setSubjectId(updated.getSubjectId())
                                    .setTicketStateId(updated.getTicketStateId())
                                    .setExternalReferenceId(externalReferenceId(updated.getExternalReferenceID()))
                                    .setExternalReferenceSource(externalReferenceSource(updated.getExternalReferenceID()))
                                    .setPriority(updated.getPriority());
                        })
                .saveAll(fieldDao, ticket -> toStoredFields(ticket, fields))
                .execute();
        return read(ticketId, true);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    public TicketSkeletonListResult older(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema,
            boolean readFields,
            final List<String> fieldNames) {

        val results = runListQuery(
                ticketFilters,
                fieldFilters,
                start,
                size,
                relevantFieldSchema,
                readFields,
                fieldNames,
                criteria -> criteria.addOrder(Order.desc(StoredTicketSkeleton.Fields.created)));
        return new TicketSkeletonListResult(
                results.getFirst()
                        .stream()
                        .sorted(Comparator.comparing(TicketSkeleton::getUpdated).reversed())
                        .toList(),
                results.getSecond()); //Keep charset consistent
    }


    @Override
    @MonitoredFunction
    @SneakyThrows
    public TicketSkeletonListResult since(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema,
            boolean readFields,
            final List<String> fieldNames) {

        val results = runListQuery(
                ticketFilters,
                fieldFilters,
                start,
                size,
                relevantFieldSchema,
                readFields,
                fieldNames,
                criteria -> criteria.addOrder(Order.desc(StoredTicketSkeleton.Fields.created)));
        return new TicketSkeletonListResult(
                results.getFirst()
                        .stream()
                        .sorted(Comparator.comparing(TicketSkeleton::getUpdated))
                        .toList(),
                results.getSecond()); //Keep charset consistent
    }


    @Override
    public TicketGroupResponse groupCount(
            String requestId,
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema,
            List<GroupingElement> groupingElements) {
        val resultCriteria = createTicketQueryCriteria(ticketFilters, fieldFilters, relevantFieldSchema);
        return new TicketGroupResponse(requestId,
                                       ConductorServerUtils.groupByAcrossShards(groupingElements,
                                                                                ticketDao::run,
                                                                                resultCriteria));
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

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRelatedTicket.RELATED_TICKET_TABLE_NAME))
    public Optional<RelatedTicket> addRelatedTicket(
            @Throws.RuntimeParam("id") String ticketId,
            @Throws.RuntimeParam("subId") String relatedToTicketId,
            TicketRelationship relationship) {
        val relatedId = ConductorServerUtils.readableId(ticketId, relatedToTicketId);
        return relatedTicketDao.createOrUpdate(ticketId,
                                               DetachedCriteria.forClass(StoredRelatedTicket.class)
                                                       .add(Property.forName(StoredRelatedTicket.Fields.relatedId)
                                                                    .eq(relatedId)),
                                               existing -> existing.setRelationship(relationship)
                                                       .setDeleted(false),
                                               () -> new StoredRelatedTicket()
                                                       .setRelatedId(relatedId)
                                                       .setTicketId(ticketId)
                                                       .setRelatedToTicketId(relatedToTicketId)
                                                       .setRelationship(relationship)
                                                       .setDeleted(false))
                .map(DBTicketStore::toRelatedTicket);
    }


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRelatedTicket.RELATED_TICKET_TABLE_NAME))
    public List<RelatedTicket> listRelatedTickets(@Throws.RuntimeParam("id") String ticketId, int from, int size) {
        return relatedTicketDao.select(ticketId,
                                       DetachedCriteria.forClass(StoredRelatedTicket.class)
                                               .add(Property.forName(StoredRelatedTicket.Fields.ticketId).eq(ticketId))
                                               .add(Property.forName(StoredRelatedTicket.Fields.deleted).eq(from)),
                                       from,
                                       size)
                .stream()
                .map(DBTicketStore::toRelatedTicket)
                .toList();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_RELATED_ENTITY_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredRelatedTicket.RELATED_TICKET_TABLE_NAME))
    public boolean deleteRelatedTicket(
            @Throws.RuntimeParam("id") String ticketId,
            @Throws.RuntimeParam("subId") String relatedToTicketId) {
        val relatedId = ConductorServerUtils.readableId(ticketId, relatedToTicketId);
        return attachmentDao.update(ticketId,
                                    DetachedCriteria.forClass(StoredRelatedTicket.class)
                                            .add(Property.forName(StoredRelatedTicket.Fields.relatedId)
                                                         .eq(relatedId)),
                                    storedRelatedTicket -> storedRelatedTicket.setDeleted(true));
    }


    @SneakyThrows
    @Throws(value = STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SKELETON_TABLE_NAME))
    @SuppressWarnings({"unchecked", "java:S107"})
    private Pair<List<TicketSkeleton>, String> runListQuery(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema,
            final boolean readFields,
            final List<String> fieldNames,
            final Consumer<DetachedCriteria> criteriaUpdater) {
        val ticketIdCriteria = DetachedCriteria.forClass(StoredTicketSkeleton.class, StoredFieldValue.Fields.ticket)
                .add(Property.forName(StoredTicketSkeleton.Fields.deleted).eq(false));
        applyTicketFilter(ticketFilters, ticketIdCriteria);
        applyFieldFilters(fieldFilters, relevantFieldSchema, ticketIdCriteria);
        ticketIdCriteria
                .setProjection(Projections.projectionList()
                                       .add(Projections.distinct(Projections.property(StoredTicketSkeleton.Fields.ticketId)))
                                       .add(Projections.property(StoredTicketSkeleton.Fields.created)));
        val pointer = TicketScrollPointer.deserializePointer(start, mapper);
        val queryResults = new ArrayList<TicketSkeleton>();
        ticketDao.runInSession(
                (shardId, session) -> {
                    val shardCriteria = ConductorServerUtils.cloneObject(ticketIdCriteria);
                    criteriaUpdater.accept(shardCriteria);
                    val executableCriteria = shardCriteria.getExecutableCriteria(session)
                            .setFirstResult(pointer.getCurrOffset(shardId))
                            .setMaxResults(size);
                    val results = (List<Object[]>) executableCriteria.list();
                    if (results.isEmpty()) {
                        return List.<StoredTicketSkeleton>of();
                    }
                    //The following is again due to a hibernate quirk which necessitates order by field to be present
                    // as a part of the projected fields for some reason. Works fine in real life, was breaking tests
                    val ids = results.stream()
                            .map(r -> (String) r[0])
                            .toList();
                    val pointQuery = DetachedCriteria.forClass(StoredTicketSkeleton.class)
                            .add(Property.forName(StoredTicketSkeleton.Fields.ticketId).in(ids));
                    //Pagination is done on IDs first and data pulled after that
                    //This is because hibernate does not allow pagination on the inner query directly
                    //Both are run in the same session
                    if (readFields && !fieldNames.isEmpty()) {
                        pointQuery.setFetchMode(StoredTicketSkeleton.Fields.fields, FetchMode.JOIN)
                                .setResultTransformer(DISTINCT_ROOT_ENTITY);
                    }
                    return (List<StoredTicketSkeleton>) pointQuery.getExecutableCriteria(session).list();
                },
                skeletonsPerShard -> {
                    skeletonsPerShard.forEach((shardIdx, result) -> {
                        result.forEach(skeleton -> queryResults.add(toSummary(skeleton, readFields, fieldNames)));
                        pointer.advance(shardIdx, result.size());// will get advanced
                    });
                    return null;
                });
        return Pair.of(queryResults, TicketScrollPointer.serializePointer(pointer, mapper));
    }

    private DetachedCriteria createTicketQueryCriteria(
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema) {
        final var filterCriteria = distinctTicketIdCriteria(ticketFilters, fieldFilters, relevantFieldSchema);
        return DetachedCriteria.forClass(StoredTicketSkeleton.class)
                .add(Property.forName(StoredTicketSkeleton.Fields.ticketId).in(filterCriteria));
    }

    @NonNull
    private DetachedCriteria distinctTicketIdCriteria(
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema) {
        val filterCriteria = DetachedCriteria.forClass(StoredTicketSkeleton.class, StoredFieldValue.Fields.ticket)
                .add(Property.forName(StoredTicketSkeleton.Fields.deleted).eq(false))
                .setProjection(Projections.distinct(Projections.property(StoredTicketSkeleton.Fields.ticketId)));
        applyTicketFilter(ticketFilters, filterCriteria);
        applyFieldFilters(fieldFilters, relevantFieldSchema, filterCriteria);
        return filterCriteria;
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
                if (assignedToGroup.isNegate()) {
                    criteria.add(Restrictions.not(
                            Restrictions.in(StoredTicketSkeleton.Fields.assignedToGroupId,
                                            assignedToGroup.getAssignedGroupIds())));
                }
                else {
                    criteria.add(
                            Restrictions.in(StoredTicketSkeleton.Fields.assignedToGroupId,
                                            assignedToGroup.getAssignedGroupIds()));
                }
                return null;
            }

            @Override
            public Void visit(TicketUnAssignedToGroup unAssignedToGroup) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.assignedToGroupId).isNull());
                return null;
            }

            @Override
            public Void visit(TicketAssignedToUser assignedToUser) {
                if (Strings.isNullOrEmpty(assignedToUser.getAssignedUserId())) {
                    criteria.add(Property.forName(StoredTicketSkeleton.Fields.assignedToUserId).isNull());
                }
                else {
                    if (assignedToUser.isNegate()) {
                        criteria.add(Property.forName(StoredTicketSkeleton.Fields.assignedToUserId).ne(
                                assignedToUser.getAssignedUserId()));
                    }
                    else {
                        criteria.add(
                                Property.forName(StoredTicketSkeleton.Fields.assignedToGroupId).eq(
                                        assignedToUser.getAssignedUserId()));
                    }
                }
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
            public Void visit(TicketPriorityIn priorityIn) {
                if (priorityIn.isNegate()) {
                    criteria.add(Restrictions.not(
                            Restrictions.in(StoredTicketSkeleton.Fields.priority, priorityIn.getPriorities())));
                }
                else {
                    criteria.add(
                            Restrictions.in(StoredTicketSkeleton.Fields.priority, priorityIn.getPriorities()));
                }
                return null;
            }

            @Override
            public Void visit(TicketsCreatedTimeWindow createdTimeWindow) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.created)
                                     .gt(new Date(createdTimeWindow.getFrom().getTime()
                                                          - createdTimeWindow.getDuration().toMilliseconds())));
                return null;
            }

            @Override
            public Void visit(TicketsUpdatedTimeWindow updatedTimeWindow) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.updated)
                                     .gt(new Date(updatedTimeWindow.getFrom().getTime()
                                                          - updatedTimeWindow.getDuration().toMilliseconds())));
                return null;
            }

            @Override
            public Void visit(TicketExternalReferenceEquals ticketExternalReferenceEquals) {
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.externalReferenceSource)
                                     .eq(ticketExternalReferenceEquals.getSource()));
                criteria.add(Property.forName(StoredTicketSkeleton.Fields.externalReferenceId)
                                     .eq(ticketExternalReferenceEquals.getValue()));
                return null;
            }
        }));
    }


    private static TicketSkeleton toSummary(
            final StoredTicketSkeleton skeleton,
            boolean readFields,
            List<String> fieldNames) {
        val fields = new ArrayList<StoredFieldValue>();
        if (readFields) {
            val requiredFields = Set.copyOf(fieldNames);
            fields.addAll(Objects.requireNonNullElse(skeleton.getFields(), List.<StoredFieldValue>of())
                                  .stream()
                                  .filter(field -> fieldNames.isEmpty() || requiredFields.contains(field.getSchemaFieldId()))
                                  .toList());
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
                                   .toList())
                .setExternalReferenceID(toExternalReferenceID(skeleton.getExternalReferenceSource(),
                                                              skeleton.getExternalReferenceId()));
    }


    private String externalReferenceSource(ExternalReferenceID reference) {
        return Optional.ofNullable(reference).map(ExternalReferenceID::getSource).orElse(null);
    }

    private String externalReferenceId(ExternalReferenceID reference) {
        return Optional.ofNullable(reference).map(ExternalReferenceID::getRefId).orElse(null);
    }

    private static ExternalReferenceID toExternalReferenceID(String extSource, String extRefId) {
        return Strings.isNullOrEmpty(extSource) ? null :
               new ExternalReferenceID(extSource, extRefId);
    }

    private static List<StoredFieldValue> toStoredFields(
            StoredTicketSkeleton ticket,
            List<TicketFieldData> fields) {
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
                .setFieldValueId(ConductorServerUtils.readableId(ticket.getTicketId(), data.getSchemaFieldId()))
                .setStoredEmbeddedFieldValue(new StoredEmbeddedFieldValue(data.getValue()))
                .setTicket(ticket)
                .setSchemaFieldId(data.getSchemaFieldId());
    }

    private static void applyFieldFilters(
            List<TicketFieldFilter> filters,
            final Map<String, FieldSchema> relevantFields,
            DetachedCriteria rootCriteria) {
        var top = rootCriteria.createCriteria(StoredTicketSkeleton.Fields.fields,
                                              StoredTicketSkeleton.Fields.fields,
                                              JoinType.LEFT_OUTER_JOIN);
        for (val filter : Objects.requireNonNullElse(filters, List.<TicketFieldFilter>of())) {
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
            @SneakyThrows
            public Void visit(TicketFieldEquals equals) {
                finalTop.add(Property.forName(StoredFieldValue.Fields.schemaFieldId).eq(equals.getFieldSchemaId()));
                switch (fieldSchema.getType()) {
                    case STRING -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.stringValue).eq(
                            equals.getValue()));
                    case CHOICE -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.choiceValue).eq(
                            List.of(equals.getValue())));
                    case BOOLEAN -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.booleanValue).eq(
                            equals.getValue()));
                    case NUMBER ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).eq(equals.getValue()));
                    case DATE ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.dateValue).eq(equals.getValue()));
                    case LOCATION -> {
                        //NO OP
                    }
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldNotEquals notEquals) {
                switch (fieldSchema.getType()) {
                    case CHOICE, STRING -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.stringValue).ne(
                            notEquals.getValue()));
                    case BOOLEAN -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.booleanValue).ne(
                            notEquals.getValue()));
                    case NUMBER -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).ne(
                            notEquals.getValue()));
                    case DATE ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.dateValue).ne(notEquals.getValue()));
                    case LOCATION -> {
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
                if (fieldSchema.getType() == FieldType.NUMBER || fieldSchema.getType() == FieldType.DATE) {
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

            @Override
            public Void visit(TicketFieldIsEmpty isEmpty) {
                val fieldName = fieldName(fieldSchema.getType());
                if (isEmpty.isNegate()) {
                    finalTop.add(fieldConstraint(fieldName).isNotNull());
                }
                else {
                    finalTop.add(fieldConstraint(fieldName).isNull());
                }
                return null;
            }


            @Override
            public Void visit(TicketFieldIn in) {
                val fieldName = fieldName(fieldSchema.getType());
                if (in.isNegate()) {
                    finalTop.add(Restrictions.not(Restrictions.in(fieldName, in.getValues())));
                }
                else {
                    finalTop.add(Restrictions.in(fieldName, in.getValues()));
                }
                return null;
            }

            private Property fieldConstraint(String actualName) {
                return Property.forName(fieldName(actualName));
            }

            private static String fieldName(FieldType fieldType) {
                return fieldName(switch (fieldType) {
                    case STRING -> StoredEmbeddedFieldValue.Fields.stringValue;
                    case CHOICE -> StoredEmbeddedFieldValue.Fields.choiceValue;
                    case BOOLEAN -> StoredEmbeddedFieldValue.Fields.booleanValue;
                    case NUMBER -> StoredEmbeddedFieldValue.Fields.numberValue;
                    case LOCATION -> StoredEmbeddedFieldValue.Fields.locationLatValue;
                    case DATE -> StoredEmbeddedFieldValue.Fields.dateValue;
                });
            }

            private static String fieldName(String actualName) {
                return String.join(".", StoredFieldValue.Fields.storedEmbeddedFieldValue, actualName);
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

    private static RelatedTicket toRelatedTicket(StoredRelatedTicket storedRelatedTicket) {
        return new RelatedTicket(storedRelatedTicket.getRelatedToTicketId(),
                                 storedRelatedTicket.getRelationship());
    }
}


