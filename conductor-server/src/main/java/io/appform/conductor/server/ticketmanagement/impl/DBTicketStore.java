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
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.ticket.TicketPriority;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.server.ticketmanagement.TicketFieldData;
import io.appform.conductor.server.ticketmanagement.TicketSkeleton;
import io.appform.conductor.server.ticketmanagement.TicketStore;
import io.appform.conductor.server.ticketmanagement.impl.models.*;
import io.appform.conductor.server.ticketmanagement.impl.models.fields.*;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.appform.conductor.model.error.ConductorErrorCode.*;

/**
 *
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBTicketStore implements TicketStore {

    private final LookupDao<StoredTicketSkeleton> ticketDao;
    private final RelationalDao<StoredTicketFieldValue> fieldDao;

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
                .saveAll(fieldDao, ticket -> toStoredFields(ticketId, fields))
                .execute();
        return read(ticketId, true);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredTicketSkeleton.TICKET_SUMMARY_TABLE_NAME))
    public Optional<TicketSkeleton> read(@Throws.RuntimeParam("id") final String ticketId, boolean readFields) {
        return ticketDao.readOnlyExecutor(ticketId)
                .readAugmentParent(fieldDao, DetachedCriteria.forClass(StoredTicketFieldValue.class)
                                           .add(Property.forName("ticketId").eq(ticketId))
                                           .add(Property.forName("deleted").eq(false)),
                                   0,
                                   Integer.MAX_VALUE,
                                   StoredTicketSkeleton::setFields)
                .execute()
                .map(DBTicketStore::toSummary);
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
                .saveAll(fieldDao, ticket -> toStoredFields(ticketId, fields))
                .execute();
        return read(ticketId, true);
    }

    private static TicketSkeleton toSummary(final StoredTicketSkeleton skeleton) {
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
                .setFields(Objects.requireNonNullElse(skeleton.getFields(), List.<StoredTicketFieldValue>of())
                                   .stream()
                                   .map(DBTicketStore::toWireField)
                                   .toList());
    }

    private static List<StoredTicketFieldValue> toStoredFields(String ticketId, List<TicketFieldData> fields) {
        return Objects.requireNonNullElse(fields, List.<TicketFieldData>of())
                .stream()
                .map(field -> toStoredField(ticketId, field))
                .toList();
    }

    private static TicketField toWireField(final StoredTicketFieldValue value) {
        return value.accept(new StoredTicketFieldValueVisitor<>() {
            @Override
            public TicketField visit(StoredTicketFieldStringValue stringValue) {
                return new TicketField(FieldType.STRING,
                                       stringValue.getSchemaFieldId(),
                                       new StringFieldValue(stringValue.getValue()),
                                       stringValue.getCreated(),
                                       stringValue.getUpdated());

            }

            @Override
            public TicketField visit(StoredTicketFieldChoiceValue choiceValue) {
                return new TicketField(FieldType.CHOICE,
                                       choiceValue.getSchemaFieldId(),
                                       new ChoiceFieldValue(choiceValue.getValue()),
                                       choiceValue.getCreated(),
                                       choiceValue.getUpdated());
            }

            @Override
            public TicketField visit(StoredTicketFieldBooleanValue booleanValue) {
                return new TicketField(FieldType.BOOLEAN,
                                       booleanValue.getSchemaFieldId(),
                                       new BooleanFieldValue(booleanValue.isValue()),
                                       booleanValue.getCreated(),
                                       booleanValue.getUpdated());
            }

            @Override
            public TicketField visit(StoredTicketFieldNumberValue numberValue) {
                return new TicketField(FieldType.NUMBER,
                                       numberValue.getSchemaFieldId(),
                                       new NumberFieldValue(numberValue.getValue()),
                                       numberValue.getCreated(),
                                       numberValue.getUpdated());

            }

            @Override
            public TicketField visit(StoredTicketFieldLocationValue locationValue) {
                return new TicketField(FieldType.LOCATION,
                                       locationValue.getSchemaFieldId(),
                                       new LocationFieldValue(locationValue.getLat(), locationValue.getLon()),
                                       locationValue.getCreated(),
                                       locationValue.getUpdated());
            }

            @Override
            public TicketField visit(StoredTicketFieldDateValue dateValue) {
                return new TicketField(FieldType.DATE,
                                       dateValue.getSchemaFieldId(),
                                       new DateFieldValue(dateValue.getValue()),
                                       dateValue.getCreated(),
                                       dateValue.getUpdated());
            }
        });
    }

    private static StoredTicketFieldValue toStoredField(
            final String ticketId,
            final TicketFieldData data) {
        return data.getValue().accept(new FieldValueVisitor<StoredTicketFieldValue>() {
                    @Override
                    public StoredTicketFieldValue visit(StringFieldValue stringFieldValue) {
                        return new StoredTicketFieldStringValue()
                                .setValue(stringFieldValue.getValue());
                    }

                    @Override
                    public StoredTicketFieldValue visit(ChoiceFieldValue choiceFieldValue) {
                        return new StoredTicketFieldChoiceValue()
                                .setValue(choiceFieldValue.getValue());
                    }

                    @Override
                    public StoredTicketFieldValue visit(BooleanFieldValue booleanFieldValue) {
                        return new StoredTicketFieldBooleanValue()
                                .setValue(booleanFieldValue.isValue());
                    }

                    @Override
                    public StoredTicketFieldValue visit(NumberFieldValue numberFieldValue) {
                        return new StoredTicketFieldNumberValue()
                                .setValue(numberFieldValue.getValue().doubleValue());
                    }

                    @Override
                    public StoredTicketFieldValue visit(LocationFieldValue locationFieldValue) {
                        return new StoredTicketFieldLocationValue()
                                .setLat(locationFieldValue.getLat())
                                .setLon(locationFieldValue.getLon());
                    }

                    @Override
                    public StoredTicketFieldValue visit(DateFieldValue dateFieldValue) {
                        return new StoredTicketFieldDateValue()
                                .setValue(dateFieldValue.getValue());
                    }
                })
                .setFieldValueId(ticketId + "-" + data.getSchemaFieldId())
                .setTicketId(ticketId)
                .setSchemaFieldId(data.getSchemaFieldId());
    }
}
