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
import io.appform.conductor.model.ticket.analytics.TicketGroupResponse;
import io.appform.conductor.model.ticket.analytics.TicketTimeSeriesResponse;
import io.appform.conductor.model.ticket.analytics.TimeResolution;
import io.appform.conductor.model.ticket.comments.Attachment;
import io.appform.conductor.model.ticket.comments.Comment;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.filter.TicketFieldFilter;
import io.appform.conductor.model.ticket.filter.TicketFieldFilterVisitor;
import io.appform.conductor.model.ticket.filter.TicketFilter;
import io.appform.conductor.model.ticket.filter.TicketFilterVisitor;
import io.appform.conductor.model.ticket.filter.fieldfilters.*;
import io.appform.conductor.model.ticket.filter.ticketfilters.*;
import io.appform.conductor.server.ticketmanagement.TicketFieldData;
import io.appform.conductor.server.ticketmanagement.TicketSkeleton;
import io.appform.conductor.server.ticketmanagement.TicketSkeletonListResult;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredAttachment;
import io.appform.conductor.server.ticketmanagement.impl.models.comments.StoredComment;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredEmbeddedFieldValue;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.dropwizard.sharding.scroll.ScrollPointer;
import io.appform.dropwizard.sharding.scroll.ScrollResult;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
                                    .setAssignedToGroupId(updated.getAssignedToGroupId())
                                    .setAssignedToUserId(updated.getAssignedToUserId())
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
    public TicketSkeletonListResult list(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema) {
        return runQuery(ticketFilters, fieldFilters, start, size, relevantFieldSchema,
                        param -> ticketDao.scrollUp(param.resultCriteria, param.pointer, param.size, "id"));
    }

    @Override
    @MonitoredFunction
    public TicketSkeletonListResult since(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema) {
        return runQuery(ticketFilters, fieldFilters, start, size, relevantFieldSchema,
                        param -> ticketDao.scrollDown(param.resultCriteria, param.pointer, param.size, "id"));
    }

    private record QueryParams(int size, DetachedCriteria resultCriteria, ScrollPointer pointer) {
    }

    @SneakyThrows
    @Throws(value = STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public TicketSkeletonListResult runQuery(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema,
            final Function<QueryParams, ScrollResult<StoredTicketSkeleton>> queryFunction) {
        val resultCriteria = createTicketQueryCriteria(ticketFilters, fieldFilters, relevantFieldSchema);
        val pointer = Strings.isNullOrEmpty(start)
                      ? null
                      : mapper.readValue(Base64.getUrlDecoder().decode(start.getBytes(StandardCharsets.UTF_8)),
                                         ScrollPointer.class);
        val results = queryFunction.apply(new QueryParams(size, resultCriteria, pointer));
        return new TicketSkeletonListResult(
                results.getResult()
                        .stream()
                        .map(ticket -> toSummary(ticket, false))
                        .toList(),
                Base64.getUrlEncoder().encodeToString(
                        mapper.writeValueAsString(results.getPointer())
                                .getBytes(StandardCharsets.UTF_8))); //Keep charset consistent
    }

    private DetachedCriteria createFlatTicketQueryCriteria(
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema) {
        val filterCriteria = DetachedCriteria.forClass(StoredTicketSkeleton.class, StoredFieldValue.Fields.ticket)
                .add(Property.forName(StoredTicketSkeleton.Fields.deleted).eq(false))
                .setProjection(Projections.distinct(Projections.property(StoredTicketSkeleton.Fields.ticketId)));
        applyTicketFilter(ticketFilters, filterCriteria);
        applyFieldFilters(fieldFilters, relevantFieldSchema, filterCriteria);
        return DetachedCriteria.forClass(StoredTicketSkeleton.class, StoredTicketSkeleton.Fields.fields)
                .add(Property.forName(StoredTicketSkeleton.Fields.ticketId).in(filterCriteria));
    }

    private DetachedCriteria createTicketQueryCriteria(
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema) {
        val filterCriteria = DetachedCriteria.forClass(StoredTicketSkeleton.class, StoredFieldValue.Fields.ticket)
                .add(Property.forName(StoredTicketSkeleton.Fields.deleted).eq(false))
                .setProjection(Projections.distinct(Projections.property(StoredTicketSkeleton.Fields.ticketId)));
        applyTicketFilter(ticketFilters, filterCriteria);
        applyFieldFilters(fieldFilters, relevantFieldSchema, filterCriteria);
        return DetachedCriteria.forClass(StoredTicketSkeleton.class)
                .add(Property.forName(StoredTicketSkeleton.Fields.ticketId).in(filterCriteria));
    }

    @Override
    @SuppressWarnings("unchecked")
    public TicketGroupResponse groupCount(
            String requestId,
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema,
            List<String> ticketPropertyNames) {
        val resultCriteria = createTicketQueryCriteria(ticketFilters, fieldFilters, relevantFieldSchema);
        val groupQuery = Projections.projectionList();
        ticketPropertyNames.forEach(property -> groupQuery.add(Projections.groupProperty(property)));
        groupQuery.add(Projections.rowCount());
        resultCriteria.setProjection(groupQuery);
        val queryResults = ticketDao.run(resultCriteria);
        var parent = new AtomicReference<TicketGroupResponse.GroupResponse>();
        queryResults.values()
                .stream()
                .map(list -> (List<Object[]>) list)
                .flatMap(List::stream)
                .forEach(groupList -> {
                    var currNode = Objects.requireNonNullElse(
                            parent.get(), new TicketGroupResponse.GroupResponse(ticketPropertyNames.get(0)));
                    for (var i = 0; i < ticketPropertyNames.size(); i++) {
                        val currValue = Objects.toString(groupList[i]);

                        if (i < ticketPropertyNames.size() - 1) {
                            val nextProperty = ticketPropertyNames.get(i + 1);
                            val nextNode =
                                    currNode.getChildren()
                                            .computeIfAbsent(currValue,
                                                             key -> new TicketGroupResponse.GroupResponse(nextProperty));
                            parent.compareAndSet(null, currNode);
                            currNode = nextNode;
                        }
                        else {
                            val count = (long) groupList[i + 1];
                            currNode.getCounts()
                                    .compute(currValue,
                                             (key, existing) -> (null == existing ? 0L : existing) + count);
                        }
                    }
                });

        return new TicketGroupResponse(requestId, parent.get());
    }

    @Override
    @SuppressWarnings("unchecked")
    public TicketTimeSeriesResponse timeSeries(
            String requestId, List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            String groupingTicketAttribute, //TODO::VALIDATE NAME AND STRING TYPE
            TimeResolution resolution,
            Map<String, FieldSchema> relevantFieldSchema) {
        val resultCriteria = createTicketQueryCriteria(ticketFilters, fieldFilters, relevantFieldSchema);
        val divisor = switch (resolution) {

            case MINUTE -> 60;
            case HOUR -> 36_00;
            case DAY -> 864_000;
            case WEEK -> 7 * 864_000;
            case MONTH -> 30 * 864_000;
        };
        if (!Strings.isNullOrEmpty(groupingTicketAttribute)) {
            resultCriteria.setProjection(Projections.projectionList()
                                                 .add(Projections.groupProperty(groupingTicketAttribute))
                                                 .add(Projections.sqlGroupProjection(
                                                         "floor(unix_timestamp(updated) / " + divisor + ") as " +
                                                                 "timestamp",
                                                         "timestamp",
                                                         new String[]{"timestamp"},
                                                         new Type[]{
                                                                 LongType.INSTANCE
                                                         }))
                                                 .add(Projections.rowCount()));
        }
        else {
            resultCriteria.setProjection(Projections.sqlGroupProjection("floor(unix_timestamp(updated) / " + divisor + ")" +
                                                                                " as timestamp, count(*) as rowcount",
                                                                        "timestamp",
                                                                        new String[]{"timestamp", "rowcount"},
                                                                        new Type[]{LongType.INSTANCE,
                                                                                LongType.INSTANCE}));
        }
        val queryResults = ticketDao.run(resultCriteria);
        if (Strings.isNullOrEmpty(groupingTicketAttribute)) {
            val counts = queryResults.values()
                    .stream()
                    .map(list -> (List<Object[]>) list)
                    .flatMap(List::stream)
                    .collect(Collectors.toMap(element -> toDate(element[0], divisor),
                                              element -> (Long) element[1],
                                              Long::sum,
                                              TreeMap::new));
            return new TicketTimeSeriesResponse(requestId, Map.of(TicketTimeSeriesResponse.DEFAULT_FIELD, counts));
        }
        else {
            val counts = queryResults.values()
                    .stream()
                    .map(list -> (List<Object[]>) list)
                    .flatMap(List::stream)
                    .collect(Collectors.groupingBy(element -> String.valueOf(element[0]),
                                                   Collectors.toMap(element -> toDate(element[1], divisor),
                                                                    element -> (Long) element[2],
                                                                    Long::sum,
                                                                    () -> (Map<Date, Long>)new TreeMap<Date, Long>())));
            return new TicketTimeSeriesResponse(requestId, counts);
        }
    }

    @NonNull
    private static Date toDate(Object element, int divisor) {
        return new Date(1000L * divisor * (Long) element);
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
                        criteria.add(Restrictions.not(
                                Restrictions.eq(StoredTicketSkeleton.Fields.assignedToUserId,
                                                assignedToUser.getAssignedUserId())));
                    }
                    else {
                        criteria.add(
                                Restrictions.eq(StoredTicketSkeleton.Fields.assignedToGroupId,
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
                            Restrictions.in(StoredTicketSkeleton.Fields.priority,
                                            priorityIn.getPriorities())));
                }
                else {
                    criteria.add(
                            Restrictions.in(StoredTicketSkeleton.Fields.priority,
                                            priorityIn.getPriorities()));
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
            public Void visit(TicketFieldEquals equals) {
                switch (fieldSchema.getType()) {
                    case CHOICE, STRING -> finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.stringValue).eq(
                            equals.getValue()));
                    case BOOLEAN ->
                            finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.booleanValue).eq(equals.getValue()));
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
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.numberValue).between(between.getMin(),
                                                                                                      between.getMax()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldDateBetween dateBetween) {
                if (fieldSchema.getType() == FieldType.DATE) {
                    finalTop.add(fieldConstraint(StoredEmbeddedFieldValue.Fields.dateValue)
                                         .between(dateBetween.getMin(), dateBetween.getMax()));
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
}


