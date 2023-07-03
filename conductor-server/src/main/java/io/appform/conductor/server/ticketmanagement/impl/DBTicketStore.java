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

import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.model.ticket.filter.*;
import io.appform.conductor.server.ticketmanagement.TicketFieldData;
import io.appform.conductor.server.ticketmanagement.TicketSkeleton;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import io.appform.conductor.server.ticketmanagement.impl.models.StoredTicketSkeleton;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.StoredFieldValue;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import io.dropwizard.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.transform.Transformers;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.UnaryOperator;

import static io.appform.conductor.model.error.ConductorErrorCode.*;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBTicketStore implements TicketStore {

    private final LookupDao<StoredTicketSkeleton> ticketDao;
    private final RelationalDao<StoredFieldValue> fieldDao;

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
                             (UnaryOperator<Criteria>) criteria -> criteria.setFetchMode("fields", FetchMode.JOIN))
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
    public List<TicketSkeleton> list(
            QueryTimeWindow timeWindow,
            final List<TicketFilterFieldBasedCriteria> filters,
            final int start,
            final int size,
            final Map<String, FieldSchema> relevantFieldSchema) {
        val window = Objects.requireNonNullElse(timeWindow,
                                                new QueryTimeWindow(Duration.days(1), new Date()));
        val criteria = DetachedCriteria.forClass(StoredTicketSkeleton.class, "ticket")
                .add(Property.forName("deleted").eq(false))
                .add(Property.forName("updated")
                             .gt(new Date(window.getFrom().getTime() - window.getDuration().toMilliseconds())))
                .setProjection(Projections.projectionList()
                                       .add(Projections.property("id"))
                                       .add(Projections.distinct(Projections.property("ticketId")))
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
        var root = criteria;
        translateFilter(filters, relevantFieldSchema, root);

        return ticketDao.scatterGather(criteria)
                .stream()
                .map(ticket -> toSummary(ticket, false))
                .toList();
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

    private static void translateFilter(
            List<TicketFilterFieldBasedCriteria> filters,
            final Map<String, FieldSchema> relevantFields,
            DetachedCriteria rootCriteria) {
        var top = rootCriteria.createCriteria("fields");
        for (val filter : filters) {
            val fieldSchema = relevantFields.get(filter.getFieldSchemaId());
            val finalTop = top;
            filter.accept(new TicketFilterVisitor<Void>() {
                @Override
                public Void visit(TicketEqualsCriteria equals) {
                    switch (fieldSchema.getType()) {
                        case STRING -> finalTop.add(Property.forName("stringValue").eq(equals.getValue()));
                        case BOOLEAN -> finalTop.add(Property.forName("booleanValue").eq(equals.getValue()));
                        case NUMBER -> finalTop.add(Property.forName("numberValue").eq(equals.getValue()));
                        case DATE -> finalTop.add(Property.forName("dateValue").eq(equals.getValue()));
                        case CHOICE, LOCATION -> {
                            //NO OP
                        }
                    }
                    return null;
                }

                @Override
                public Void visit(TicketNotEqualsCriteria notEquals) {
                    switch (fieldSchema.getType()) {
                        case STRING -> finalTop.add(Property.forName("stringValue").ne(notEquals.getValue()));
                        case BOOLEAN -> finalTop.add(Property.forName("booleanValue").ne(notEquals.getValue()));
                        case NUMBER -> finalTop.add(Property.forName("numberValue").ne(notEquals.getValue()));
                        case DATE -> finalTop.add(Property.forName("dateValue").ne(notEquals.getValue()));
                        case CHOICE, LOCATION -> {
                            //NO OP
                        }
                    }
                    return null;
                }
            });
            top = top.createCriteria("ticket")
                    .createCriteria("fields");

        }
    }
}


