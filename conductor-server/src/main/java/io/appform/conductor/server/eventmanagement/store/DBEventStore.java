/*
 * Copyright (c) 2023 santanu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package io.appform.conductor.server.eventmanagement.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.events.analytics.impl.EventGroupResponse;
import io.appform.conductor.model.ticket.analytics.GroupingElement;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.server.eventmanagement.EventStore;
import io.appform.conductor.model.events.analytics.EventFilters;
import io.appform.conductor.model.events.analytics.impl.EventListResponse;
import io.appform.conductor.server.eventmanagement.store.models.StoredEvent;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.conductor.server.utils.Pair;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.dropwizard.sharding.scroll.ScrollPointer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Stores events in DB.
 * Fields are stored directly for {@link Event}. The whole event is serialized and stored
 * Event shard routing is mostly on referred object ids as we expect most queries to be around that.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBEventStore implements EventStore {

    private final RelationalDao<StoredEvent> eventDao;
    private final ObjectMapper mapper;

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredEvent.EVENTS_TABLE_NAME))
    public boolean save(@Throws.RuntimeParam("id") String eventId, Event event) {
        val source = serialize(event);
        return eventDao.save(
                        partitionKey(event),
                        new StoredEvent()
                                .setId(event.getId())
                                .setType(event.getType())
                                .setObjectType(event.getObjectType())
                                .setObjectId(event.getObjectId())
                                .setUserId(event.getUserId())
                                .setDate(event.getDate())
                                .setSource(source.getFirst())
                                .setSourceFormat(source.getSecond()))
                .isPresent();
    }

    @Override
    @SneakyThrows
    public EventListResponse list(EventFilters filters, String nextPointer, int size) {
        val criteria = createCriteria(filters);
        val ptr = Strings.isNullOrEmpty(nextPointer)
                  ? null
                  : mapper.readValue(nextPointer, ScrollPointer.class);
        //TODO::YEAR etc
        val result = eventDao.scrollUp(criteria, ptr, size, StoredEvent.Fields.date);
        return new EventListResponse(result.getResult()
                                           .stream()
                                           .map(e -> toEvent(e.getSource(), e.getSourceFormat()))
                                           .toList(),
                                     mapper.writeValueAsString(result.getPointer()));
    }

    @Override
    public EventGroupResponse groupBy(EventFilters filters, List<GroupingElement> groupingElements) {
        val criteria = createCriteria(filters);
        return new EventGroupResponse(ConductorServerUtils.groupByAcrossShards(groupingElements, eventDao::run, criteria));
    }

    private static String partitionKey(final Event event) {
        return event.getObjectType() + "-" + event.getObjectId();
    }

    @SneakyThrows
    private Pair<String, StoredEvent.SourceFmt> serialize(Event event) {
        return Pair.of(mapper.writeValueAsString(event), StoredEvent.SourceFmt.V1);
    }

    @SneakyThrows
    private Event toEvent(final String data, final StoredEvent.SourceFmt format) {
        return switch (format) {
            case V1 -> mapper.readValue(data, Event.class);
        };
    }

    private static DetachedCriteria createCriteria(EventFilters filters) {
        val criteria = DetachedCriteria.forClass(StoredEvent.class);
        if (null != filters.getEventTypes() && !filters.getEventTypes().isEmpty()) {
            criteria.add(Property.forName(StoredEvent.Fields.type).in(filters.getEventTypes()));
        }
        if (null != filters.getReference()) {
            criteria.add(Property.forName(StoredEvent.Fields.objectId).eq(filters.getReference().objectId()))
                    .add(Property.forName(StoredEvent.Fields.objectType).eq(filters.getReference().type()));
        }
        else if(null != filters.getReferenceType()) {
            criteria.add(Property.forName(StoredEvent.Fields.objectType).eq(filters.getReferenceType()));
        }
        if (null != filters.getUserIds()  && !filters.getUserIds().isEmpty()) {
            criteria.add(Property.forName(StoredEvent.Fields.userId).in(filters.getUserIds()));
        }
        if (null != filters.getTimeWindow()) {
            val startDate = Objects.requireNonNullElse(filters.getTimeWindow().getFrom(), new Date());
            criteria.add(Property.forName(StoredEvent.Fields.date)
                                 .between(startDate,
                                          new Date(startDate.getTime() - filters.getTimeWindow()
                                                  .getDuration()
                                                  .toMillis())));
        }
        return criteria;
    }
}
