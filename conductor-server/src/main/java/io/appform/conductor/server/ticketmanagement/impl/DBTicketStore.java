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
import com.google.common.collect.TreeBasedTable;
import com.google.common.net.MediaType;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
                .map(ticket -> toSummary(ticket, true, List.of()));
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
                            val updated = updater.apply(toSummary(ticket, false, List.of()));
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
            final Map<String, FieldSchema> relevantFieldSchema,
            boolean readFields,
            final List<String> fieldNames) {
        return runQuery(ticketFilters,
                        fieldFilters,
                        start,
                        size * ((!readFields || relevantFieldSchema.isEmpty()) ? 1 : relevantFieldSchema.size()),
                        relevantFieldSchema,
                        param -> ticketDao.scrollUp(param.resultCriteria, param.pointer, param.size, "id"),
                        readFields,
                        fieldNames);
    }

    @Override
    @MonitoredFunction
    public TicketSkeletonListResult since(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema,
            boolean readFields,
            final List<String> fieldsToBeFetched) {
        return runQuery(ticketFilters,
                        fieldFilters,
                        start,
                        size,
                        relevantFieldSchema,
                        param -> ticketDao.scrollDown(param.resultCriteria, param.pointer, param.size, "id"),
                        readFields,
                        fieldsToBeFetched);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TicketGroupResponse groupCount(
            String requestId,
            List<TicketFilter> ticketFilters,
            List<TicketFieldFilter> fieldFilters,
            Map<String, FieldSchema> relevantFieldSchema,
            List<GroupingElement> groupingElements) {
        val resultCriteria = createTicketQueryCriteria(ticketFilters, fieldFilters, relevantFieldSchema);
        val groupQuery = Projections.projectionList();
        val aliasedElements = groupingElements.stream()
                .map(groupingElement -> groupingElement.accept(new GroupingElementVisitor<GroupingElement>() {
                    @Override
                    public GroupingElement visit(ColumnGroupingElement columnGroupingElement) {
                        return new ColumnGroupingElement(columnGroupingElement.getAttribute(),
                                                         Objects.requireNonNullElse(columnGroupingElement.getAlias(),
                                                                                    columnGroupingElement.getAttribute()));
                    }

                    @Override
                    public GroupingElement visit(TimeBucketGroupingElement timeBucketGroupingElement) {
                        return new TimeBucketGroupingElement(timeBucketGroupingElement.getDateAttribute(),
                                                             timeBucketGroupingElement.getResolution(),
                                                             Objects.requireNonNullElse(timeBucketGroupingElement.getAlias(),
                                                                                        "timestamp"));
                    }
                }))
                .toList();
        aliasedElements.forEach(element -> element.accept(new GroupingElementVisitor<Void>() {
            @Override
            public Void visit(ColumnGroupingElement columnGroupingElement) {
                groupQuery.add(Projections.alias(Projections.groupProperty(columnGroupingElement.getAttribute()),
                                                 columnGroupingElement.getAlias()));
                return null;
            }

            @Override
            public Void visit(TimeBucketGroupingElement timeBucketGroupingElement) {
                val divisor = switch (timeBucketGroupingElement.getResolution()) {
                    case MINUTE -> 60;
                    case HOUR -> 36_00;
                    case DAY -> 864_000;
                    case WEEK -> 7 * 864_000;
                    case MONTH -> 30 * 864_000;
                };
                groupQuery.add(Projections.sqlGroupProjection(
                        "floor(unix_timestamp(" + timeBucketGroupingElement.getDateAttribute() + ") / " + divisor +
                                ") as " +
                                timeBucketGroupingElement.getAlias(),
                        timeBucketGroupingElement.getAlias(),
                        new String[]{timeBucketGroupingElement.getAlias()},
                        new Type[]{LongType.INSTANCE}));
                return null;
            }
        }));
        groupQuery.add(Projections.rowCount());
        resultCriteria.setProjection(groupQuery);
        val queryResults = ticketDao.run(resultCriteria);
        val rows = queryResults.values()
                .stream()
                .map(list -> (List<Object[]>) list)
                .flatMap(List::stream)
                .toList();
        val aliases = ConductorServerUtils.aliasesForGroupingElements(aliasedElements);
        val table = parseGroupResponse(aliasedElements, rows);

        return new TicketGroupResponse(requestId, table);
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

    private record QueryParams(int size, DetachedCriteria resultCriteria, ScrollPointer pointer) {
    }

    @SneakyThrows
    @Throws(value = STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    private TicketSkeletonListResult runQuery(
            final List<TicketFilter> ticketFilters,
            final List<TicketFieldFilter> fieldFilters,
            final String start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema,
            final Function<QueryParams, ScrollResult<StoredTicketSkeleton>> queryFunction,
            boolean fieldsToBeRead,
            final List<String> fieldNames) {
        val resultCriteria = createTicketQueryCriteria(ticketFilters, fieldFilters, relevantFieldSchema);
        if (fieldsToBeRead && !fieldNames.isEmpty()) {
            resultCriteria.setFetchMode(StoredTicketSkeleton.Fields.fields, FetchMode.JOIN)
                    .setResultTransformer(DISTINCT_ROOT_ENTITY);
            //TODO::FETCH RELEVANT FIELDS ONLY
            /*val fc = DetachedCriteria.forClass(StoredFieldValue.class)
                    .add(Restrictions.in(StoredFieldValue.Fields.schemaFieldId, fieldsToBeFetched));*/
            /*resultCriteria.createCriteria(StoredTicketSkeleton.Fields.fields,
                                          "f",
                                          JoinType.LEFT_OUTER_JOIN,
                                          Restrictions.in(StoredFieldValue.Fields.schemaFieldId, fieldsToBeFetched));*/
//            resultCriteria.add(Property.forName(StoredTicketSkeleton.Fields.fields).eq(fc));

//                    ;
            /*resultCriteria
                    .setResultTransformer(DISTINCT_ROOT_ENTITY)
                    .createCriteria(StoredTicketSkeleton.Fields.fields, "f", JoinType.LEFT_OUTER_JOIN, Restrictions
                    .in(StoredFieldValue.Fields.schemaFieldId, fieldsToBeFetched))
                    ;
                    */


        }
        val pointer = Strings.isNullOrEmpty(start)
                      ? null
                      : mapper.readValue(Base64.getUrlDecoder().decode(start.getBytes(StandardCharsets.UTF_8)),
                                         ScrollPointer.class);
        val results = queryFunction.apply(new QueryParams(size, resultCriteria, pointer));
        return new TicketSkeletonListResult(
                results.getResult()
                        .stream()
                        .map(ticket -> toSummary(ticket, fieldsToBeRead, fieldNames))
                        .toList(),
                Base64.getUrlEncoder().encodeToString(
                        mapper.writeValueAsString(results.getPointer())
                                .getBytes(StandardCharsets.UTF_8))); //Keep charset consistent
    }

    private static TreeBasedTable<Integer, String, Object> parseGroupResponse(
            List<GroupingElement> groupingElements,
            List<Object[]> rows) {
        val output = new HashMap<List<String>, Long>();
        val formats = EnumSet.allOf(TimeResolution.class)
                .stream()
                .collect(Collectors.toMap(Function.identity(), resolution -> switch (resolution) {
                    case MINUTE -> new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    case HOUR -> new SimpleDateFormat("yyyy-MM-dd HH");
                    case DAY -> new SimpleDateFormat("yyyy-MM-dd");
                    case WEEK -> new SimpleDateFormat("yyyy ww");
                    case MONTH -> new SimpleDateFormat("yyyy-MM");
                }));
        for (val row : rows) {
            val key = new ArrayList<String>(row.length - 1);
            for (var colId = 0; colId < row.length - 1; colId++) {
                int finalColId = colId;
                key.add(groupingElements.get(colId)
                                .accept(new GroupingElementVisitor<String>() {
                                    @Override
                                    public String visit(ColumnGroupingElement columnGroupingElement) {
                                        return Objects.toString(row[finalColId]);
                                    }

                                    @Override
                                    public String visit(TimeBucketGroupingElement timeBucketGroupingElement) {
                                        val divisor = switch (timeBucketGroupingElement.getResolution()) {

                                            case MINUTE -> 60;
                                            case HOUR -> 36_00;
                                            case DAY -> 864_000;
                                            case WEEK -> 7 * 864_000;
                                            case MONTH -> 30 * 864_000;
                                        };
                                        return formats.get(timeBucketGroupingElement.getResolution())
                                                .format(toDate(row[finalColId], divisor));
                                    }
                                }));
            }
            val oldValue = output.computeIfAbsent(key, k -> 0L);
            output.put(key, oldValue + (long) row[row.length - 1]);
        }
        val table = TreeBasedTable.<Integer, String, Object>create();
        val rowIdx = new AtomicInteger(0);
        output
                .forEach((keys, value) -> {
                    val row = table.row(rowIdx.incrementAndGet());
                    for (int i = 0; i < keys.size(); i++) {
                        row.put(groupingElements.get(i).getAlias(), keys.get(i));
                    }
                    row.put("count", value);
                });
        return table;
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
        }));
    }


    @NonNull
    private static Date toDate(Object element, int divisor) {
        return new Date(1000L * divisor * (Long) element);
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
}


