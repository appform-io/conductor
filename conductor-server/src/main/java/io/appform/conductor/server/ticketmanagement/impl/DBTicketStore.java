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
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.fields.impl.*;
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
import org.hibernate.transform.Transformers;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;

import static io.appform.conductor.model.error.ConductorErrorCode.*;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBTicketStore implements TicketStore {
    private static final String FIELDS_VALUE_FIELD_NAME = "fields";
    private static final String TICKETS_VALUE_FIELD_NAME = "ticket";
    private static final String STRING_VALUE_FIELD_NAME = "stringValue";
    private static final String NUMBER_VALUE_FIELD_NAME = "numberValue";
    private static final String BOOLEAN_VALUE_FIELD_NAME = "booleanValue";
    private static final String LOCATION_LAT_VALUE_FIELD_NAME = "locationLatValue";
    private static final String LOCATION_LON_VALUE_FIELD_NAME = "locationLonValue";
    private static final String CHOICE_VALUE_FIELD_NAME = "choiceValue";
    private static final String DATE_VALUE_FIELD_NAME = "dateValue";

    private final LookupDao<StoredTicketSkeleton> ticketDao;
    private final RelationalDao<StoredFieldValue> fieldDao;
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
                             (UnaryOperator<Criteria>) criteria -> criteria.setFetchMode(FIELDS_VALUE_FIELD_NAME,
                                                                                         FetchMode.JOIN))
                .map(ticket -> toSummary(ticket, true));
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
        val criteria = DetachedCriteria.forClass(StoredTicketSkeleton.class, TICKETS_VALUE_FIELD_NAME)
                .add(Property.forName("deleted").eq(false))
                .setProjection(Projections.projectionList()
                                       .add(Projections.distinct(Projections.property("ticketId")))
                                       .add(Projections.property("id"))
                                       .add(Projections.property("ticketId"))
                                       .add(Projections.property("title"))
                                       .add(Projections.property("description"))
                                       .add(Projections.property("workflowId"))
                                       .add(Projections.property("createdByUserId"))
                                       .add(Projections.property("assignedToGroupId"))
                                       .add(Projections.property("assignedToUserId"))
                                       .add(Projections.property("subjectId"))
                                       .add(Projections.property("ticketStateId"))
                                       .add(Projections.property("priority"))
                                       .add(Projections.property("deleted"))
                                       .add(Projections.property("created"))
                                       .add(Projections.property("updated"))
                              )
                .setResultTransformer(Transformers.aliasToBean(StoredTicketSkeleton.class));
        applyTicketFilter(ticketFilters, criteria);
        applyFieldFilters(fieldFilters, relevantFieldSchema, criteria);
        val pointer = Strings.isNullOrEmpty(start)
                      ? null
                      : mapper.readValue(Base64.getUrlDecoder().decode(start.getBytes(StandardCharsets.UTF_8)),
                                         ScrollPointer.class);
        val results = ticketDao.scrollUp(criteria, pointer, size, "id");
        return new TicketSkeletonListResult(
                results.getResult()
                        .stream()
                        .map(ticket -> toSummary(ticket, false))
                        .toList(),
                Base64.getUrlEncoder().encodeToString(
                        mapper.writeValueAsString(results.getPointer())
                                .getBytes(StandardCharsets.UTF_8))); //Keep charset consistent
    }

    private void applyTicketFilter(List<TicketFilter> ticketFilters, DetachedCriteria criteria) {
        ticketFilters.forEach(filter -> filter.accept(new TicketFilterVisitor<Void>() {
            @Override
            public Void visit(TicketWorkflowEquals workflowEquals) {
                criteria.add(Property.forName("workflowId").eq(workflowEquals.getWorkflowId()));
                return null;
            }

            @Override
            public Void visit(TicketCreatedBy createdBy) {
                criteria.add(Property.forName("createdByUserId").eq(createdBy.getCreateByUserId()));
                return null;
            }

            @Override
            public Void visit(TicketAssignedToGroup assignedToGroup) {
                criteria.add(Property.forName("assignedToGroupId").eq(assignedToGroup.getAssignedGroupId()));
                return null;
            }

            @Override
            public Void visit(TicketUnAssignedToGroup unAssignedToGroup) {
                criteria.add(Property.forName("assignedToGroupId").isNull());
                return null;
            }

            @Override
            public Void visit(TicketAssignedToUser assignedToUser) {
                criteria.add(Property.forName("assignedToUserId").eq(assignedToUser.getAssignedUserId()));
                return null;
            }

            @Override
            public Void visit(TicketUnAssignedToUser unAssignedToUser) {
                criteria.add(Property.forName("assignedToUserId").isNull());
                return null;
            }

            @Override
            public Void visit(TicketSubjectEquals subjectEquals) {
                criteria.add(Property.forName("subjectId").eq(subjectEquals.getSubjectId()));
                return null;
            }

            @Override
            public Void visit(TicketStateEquals stateEquals) {
                criteria.add(Property.forName("ticketStateId").eq(stateEquals.getStateId()));
                return null;
            }

            @Override
            public Void visit(TicketPriorityEquals priorityEquals) {
                criteria.add(Property.forName("priority").eq(priorityEquals.getPriority()));
                return null;
            }

            @Override
            public Void visit(TicketsCreatedTimeWindow createdTimeWindow) {
                criteria.add(Property.forName("created")
                                     .gt(new Date(createdTimeWindow.getFrom()
                                                          .getTime() - createdTimeWindow.getDuration()
                                             .toMilliseconds())));
                return null;
            }

            @Override
            public Void visit(TicketsUpdatedTimeWindow updatedTimeWindow) {
                criteria.add(Property.forName("updated")
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
        return switch (value.getType()) {

            case STRING -> new TicketField(FieldType.STRING,
                                           value.getSchemaFieldId(),
                                           new StringFieldValue(value.getStringValue()),
                                           value.getCreated(),
                                           value.getUpdated());
            case CHOICE -> new TicketField(FieldType.CHOICE,
                                           value.getSchemaFieldId(),
                                           new ChoiceFieldValue(value.getChoiceValue()),
                                           value.getCreated(),
                                           value.getUpdated());
            case BOOLEAN -> new TicketField(FieldType.BOOLEAN,
                                            value.getSchemaFieldId(),
                                            new BooleanFieldValue(value.isBooleanValue()),
                                            value.getCreated(),
                                            value.getUpdated());
            case NUMBER -> new TicketField(FieldType.NUMBER,
                                           value.getSchemaFieldId(),
                                           new NumberFieldValue(value.getNumberValue()),
                                           value.getCreated(),
                                           value.getUpdated());
            case LOCATION -> new TicketField(FieldType.LOCATION,
                                             value.getSchemaFieldId(),
                                             new LocationFieldValue(value.getLocationLatValue(),
                                                                    value.getLocationLonValue()),
                                             value.getCreated(),
                                             value.getUpdated());
            case DATE -> new TicketField(FieldType.DATE,
                                         value.getSchemaFieldId(),
                                         new DateFieldValue(value.getDateValue()),
                                         value.getCreated(),
                                         value.getUpdated());
        };
    }

    private static StoredFieldValue toStoredField(
            final StoredTicketSkeleton ticket,
            final TicketFieldData data) {
        val fieldValue = new StoredFieldValue()
                .setFieldValueId(ticket.getTicketId() + "-" + data.getSchemaFieldId())
                .setType(data.getValue().getType())
                .setTicket(ticket)
                .setSchemaFieldId(data.getSchemaFieldId());
        data.getValue().accept(new FieldValueVisitor<Void>() {
            @Override
            public Void visit(StringFieldValue stringFieldValue) {
                fieldValue.setStringValue(stringFieldValue.getValue());
                return null;
            }

            @Override
            public Void visit(ChoiceFieldValue choiceFieldValue) {
                fieldValue.setChoiceValue(choiceFieldValue.getValue());
                return null;
            }

            @Override
            public Void visit(BooleanFieldValue booleanFieldValue) {
                fieldValue.setBooleanValue(booleanFieldValue.isValue());
                return null;
            }

            @Override
            public Void visit(NumberFieldValue numberFieldValue) {
                fieldValue.setNumberValue(numberFieldValue.getValue());
                return null;
            }

            @Override
            public Void visit(LocationFieldValue locationFieldValue) {
                fieldValue.setLocationLatValue(locationFieldValue.getLat())
                        .setLocationLatValue(locationFieldValue.getLon());
                return null;
            }

            @Override
            public Void visit(DateFieldValue dateFieldValue) {
                fieldValue.setDateValue(dateFieldValue.getValue());
                return null;
            }
        });
        return fieldValue;
    }

    private static void applyFieldFilters(
            List<TicketFieldFilter> filters,
            final Map<String, FieldSchema> relevantFields,
            DetachedCriteria rootCriteria) {
        var top = rootCriteria.createCriteria(FIELDS_VALUE_FIELD_NAME);
        for (val filter : filters) {
            val fieldSchema = relevantFields.get(filter.getFieldSchemaId());
            val finalTop = top;
            augmentFilter(filter, fieldSchema, finalTop);
            top = top.createCriteria(TICKETS_VALUE_FIELD_NAME)
                    .createCriteria(FIELDS_VALUE_FIELD_NAME);

        }
    }

    @SuppressWarnings("java:S3776")
    private static void augmentFilter(TicketFieldFilter filter, FieldSchema fieldSchema, DetachedCriteria finalTop) {
        filter.accept(new TicketFieldFilterVisitor<Void>() {

            @Override
            public Void visit(TicketFieldEquals equals) {
                switch (fieldSchema.getType()) {
                    case STRING -> finalTop.add(Property.forName(STRING_VALUE_FIELD_NAME).eq(equals.getValue()));
                    case BOOLEAN -> finalTop.add(Property.forName(BOOLEAN_VALUE_FIELD_NAME).eq(equals.getValue()));
                    case NUMBER -> finalTop.add(Property.forName(NUMBER_VALUE_FIELD_NAME).eq(equals.getValue()));
                    case DATE -> finalTop.add(Property.forName(DATE_VALUE_FIELD_NAME).eq(equals.getValue()));
                    case CHOICE, LOCATION -> {
                        //NO OP
                    }
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldNotEquals notEquals) {
                switch (fieldSchema.getType()) {
                    case STRING -> finalTop.add(Property.forName(STRING_VALUE_FIELD_NAME).ne(notEquals.getValue()));
                    case BOOLEAN -> finalTop.add(Property.forName(BOOLEAN_VALUE_FIELD_NAME).ne(notEquals.getValue()));
                    case NUMBER -> finalTop.add(Property.forName(NUMBER_VALUE_FIELD_NAME).ne(notEquals.getValue()));
                    case DATE -> finalTop.add(Property.forName(DATE_VALUE_FIELD_NAME).ne(notEquals.getValue()));
                    case CHOICE, LOCATION -> {
                        //NO OP
                    }
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldGreater greater) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(Property.forName(NUMBER_VALUE_FIELD_NAME).gt(greater.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldGreaterEquals greaterEquals) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(Property.forName(NUMBER_VALUE_FIELD_NAME).ge(greaterEquals.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldLesser lesser) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(Property.forName(NUMBER_VALUE_FIELD_NAME).lt(lesser.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldLesserEquals lesserEquals) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(Property.forName(NUMBER_VALUE_FIELD_NAME).le(lesserEquals.getValue()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldBetween between) {
                if (fieldSchema.getType() == FieldType.NUMBER) {
                    finalTop.add(Property.forName(NUMBER_VALUE_FIELD_NAME).between(between.getMin(), between.getMax()));
                }
                return null;
            }

            @Override
            public Void visit(TicketFieldContainsChoices containsChoices) {
                if (fieldSchema.getType() == FieldType.CHOICE) {
                    val query = Restrictions.conjunction();
                    containsChoices.getChoices()
                            .forEach(choice -> query.add(Property.forName(CHOICE_VALUE_FIELD_NAME)
                                                                 .like(choice, MatchMode.ANYWHERE)));
                    finalTop.add(query);
                }
                return null;
            }
        });
    }
}


