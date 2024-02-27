/*
 * Copyright (c) 2024 santanu
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

package io.appform.conductor.server.utils;

import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.events.Event;
import io.appform.conductor.model.events.analytics.EventQueryResponse;
import io.appform.conductor.model.events.analytics.EventQueryResponseVisitor;
import io.appform.conductor.model.events.analytics.impl.EventGroupResponse;
import io.appform.conductor.model.events.analytics.impl.EventListResponse;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.TicketSummary;
import io.appform.conductor.model.ticket.analytics.*;
import io.appform.conductor.model.ticket.fields.FieldValue;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.server.auth.ConductorUser;
import io.appform.conductor.server.parser.CQLEngine;
import io.appform.conductor.server.ticketmanagement.statemachine.models.TicketStateMachineContext;
import io.appform.conductor.server.usermanagement.CurrentUserSessionStore;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cronutils.model.CronType.QUARTZ;

/**
 *
 */
@UtilityClass
public class ConductorServerUtils {
    public static String errorMessage(Throwable t) {
        var root = t;
        while (null != root.getCause()) {
            root = root.getCause();
        }
        return Strings.isNullOrEmpty(root.getMessage())
               ? CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, root.getClass().getSimpleName())
               : root.getMessage();
    }

    public static int currentWeek() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        return c.getWeekYear();
    }

    public static String lowerSnake(final String value) {
        return value
                .trim()
                .toLowerCase()
                .replaceAll("\\p{Space}", "_")
                .replaceAll("\\p{Punct}", "_");
    }

    public static String upperSnake(final String value) {
        return value
                .trim()
                .toUpperCase()
                .replaceAll("\\p{Space}", "_")
                .replaceAll("\\p{Punct}", "_");
    }

    public static String readableId(final String... values) {
        val value = String.join("_", values);
        return lowerSnake(value).toUpperCase();
    }

    public static String readableId(final String value) {
        return lowerSnake(value).toUpperCase();
    }


    public static String operatingUserId() {
        return CurrentUserSessionStore.get()
                .map(user -> user.getUser().getSummary().getId())
                .orElse(null);
    }

    public static void configureMapper(ObjectMapper objectMapper) {
        objectMapper.registerModule(new ParameterNamesModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static void ensure(boolean condition, ConductorErrorCode errorCode, String message) {
        if (!condition) {
            throw new ConductorException(errorCode, Map.of("message", message), null);
        }
    }

    public static void ensure(boolean condition, ConductorErrorCode errorCode, Map<String, Object> context) {
        if (!condition) {
            throw new ConductorException(errorCode, context, null);
        }
    }

    public static void notNull(Object object, ConductorErrorCode errorCode, String message) {
        if (null == object) {
            throw new ConductorException(errorCode, Map.of("message", message), null);
        }
    }

    public static void logicalError(ConductorErrorCode errorCode, String message) {
        throw new ConductorException(errorCode, Map.of("message", message), null);
    }

    public static void logicalError(ConductorErrorCode errorCode) {
        logicalError(errorCode, Map.of());
    }

    public static void ensureNonNull(@Nullable final Object testObject, ConductorErrorCode errorCode) {
        ensureNonNull(testObject, errorCode, Map.of());
    }

    public static void ensureNonNull(
            @Nullable final Object testObject,
            ConductorErrorCode errorCode,
            Map<String, Object> context) {
        ensureCondition(null != testObject, errorCode, context);
    }

    public static void ensureCondition(boolean condition, ConductorErrorCode errorCode, Map<String, Object> context) {
        if (!condition) {
            logicalError(errorCode, context);
        }
    }

    public static void ensureCondition(boolean condition, ConductorErrorCode errorCode) {
        ensureCondition(condition, errorCode, Map.of());
    }

    public static void logicalError(ConductorErrorCode errorCode, Map<String, Object> context) {
        throw new ConductorException(errorCode, context, null);
    }

    public static <T> List<T> joinLists(List<T>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).toList();
    }

    public static JsonNode ticketToJsonNode(
            final ObjectMapper mapper,
            final TicketDetails ticket,
            final Schema schema) {
        val fieldSchemaMap = schema.getFields().stream().collect(Collectors.toUnmodifiableMap(FieldSchema::getId,
                                                                                              Function.identity()));
        val node = mapper.createObjectNode();
        node.set("summary", mapper.valueToTree(ticket.getSummary()));
        val fields = mapper.createObjectNode();
        node.set("fields", fields);
        ticket.getFields()
                .forEach(field -> {
                    val fieldSchema = fieldSchemaMap.get(field.getFieldSchemaId());
                    fields.set(fieldSchema.getName(),
                               mapValueToJsonNode(mapper, fieldSchema, field));
                });
        return node;
    }

    public static JsonNode mapValueToJsonNode(ObjectMapper mapper, FieldSchema fieldSchema, TicketField field) {
        return mapValueToJsonNode(mapper, fieldSchema, field.getFieldValue());
    }

    public static JsonNode mapValueToJsonNode(ObjectMapper mapper, FieldSchema fieldSchema, FieldValue fieldValue) {
        return fieldValue.accept(new FieldValueVisitor<>() {
            @Override
            public JsonNode visit(StringFieldValue stringFieldValue) {
                return mapper.createObjectNode()
                        .textNode(stringFieldValue.getValue());
            }

            @Override
            public JsonNode visit(ChoiceFieldValue choiceFieldValue) {
                val selection = Objects.requireNonNullElse(choiceFieldValue.getValue(), List.<String>of());
                if (fieldSchema.isAllowMultiple()) {
                    val choices = mapper.createArrayNode();
                    selection.forEach(choices::add);
                    return choices;
                }

                return selection.stream()
                        .limit(1)
                        .findFirst()
                        .map(choice -> (JsonNode) mapper.createObjectNode().textNode(choice))
                        .orElse(mapper.nullNode());
            }

            @Override
            public JsonNode visit(BooleanFieldValue booleanFieldValue) {
                return mapper.createObjectNode()
                        .booleanNode(booleanFieldValue.isValue());
            }

            @Override
            public JsonNode visit(NumberFieldValue numberFieldValue) {
                return mapper.createObjectNode()
                        .numberNode(numberFieldValue.getValue());
            }

            @Override
            public JsonNode visit(LocationFieldValue locationFieldValue) {
                val loc = mapper.createObjectNode();
                loc.set("lat", mapper.createObjectNode().numberNode(locationFieldValue.getLat()));
                loc.set("lon", mapper.createObjectNode().numberNode(locationFieldValue.getLon()));
                return loc;
            }

            @Override
            public JsonNode visit(DateFieldValue dateFieldValue) {
                return mapper.createObjectNode()
                        .numberNode(dateFieldValue.getValue()
                                            .toInstant()
                                            .toEpochMilli());
            }
        });
    }

    public static CloseableHttpClient createHttpClient() {
        val connectionTimeout = Timeout.of(Duration.ofSeconds(3));
        val connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(100);
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                                                             .setConnectTimeout(connectionTimeout)
                                                             .setSocketTimeout(connectionTimeout)
                                                             .setValidateAfterInactivity(TimeValue.ofSeconds(10))
                                                             .setTimeToLive(TimeValue.ofHours(1))
                                                             .build());
        val requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectionTimeout)
                .setResponseTimeout(Timeout.of(Duration.ofSeconds(5)))
                .build();
        return HttpClients.custom()
                .disableRedirectHandling()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public static Response render(final TemplateView view) {
        return render(view, Map.of());
    }

    public static Response render(final TemplateView view, Map<String, List<Object>> headers) {
        val responseBuilder = Response.ok(view);
        if (!headers.isEmpty()) {
            headers.forEach((name, values) -> values.forEach(value -> responseBuilder.header(name, value)));
        }
        return responseBuilder.build();
    }

    public static Response redirect(final String uri) {
        return Response.seeOther(URI.create(uri)).build();
    }

    public static WebApplicationException fail(final String message, final String uri) {
        throw new WebApplicationException(message,
                                          failureResponse(message, uri));
    }

    public static Response failureResponse(String message, String uri) {
        return Response.seeOther(URI.create(uri))
                .cookie(new NewCookie(
                        "server-error-message",
                        message,
                        "/",
                        null,
                        Cookie.DEFAULT_VERSION,
                        null,
                        NewCookie.DEFAULT_MAX_AGE,
                        null,
                        false,
                        false))
                .build();
    }

    public static String userId(ConductorUser user) {
        return user.getUserSession().getUser().getSummary().getId();
    }

    public static TicketDetails ticketDetails(TicketStateMachineContext ticketStateMachineContext) {
        val skeleton = ticketStateMachineContext.getTicketSkeleton();
        return new TicketDetails(new TicketSummary(skeleton.getTicketId(),
                                                   skeleton.getTitle(),
                                                   skeleton.getDescription(),
                                                   skeleton.getWorkflowId(),
                                                   ticketStateMachineContext.getTicketCreatedBy(),
                                                   ticketStateMachineContext.getTicketAssignedToGroup(),
                                                   ticketStateMachineContext.getTicketAssignedToUser(),
                                                   ticketStateMachineContext.getSubject(),
                                                   ticketStateMachineContext.getWorkflow()
                                                           .getStates()
                                                           .get(skeleton.getTicketStateId()),
                                                   skeleton.getPriority(),
                                                   skeleton.getExternalReferenceID(),
                                                   skeleton.getCreated(),
                                                   skeleton.getUpdated()),
                                 skeleton.getFields(),
                                 List.of()); //TODO::ACTION
    }

    public static Date htmlDateToDate(String date) {
        return Strings.isNullOrEmpty(date)
               ? null
               : Date.from(LocalDate.parse(date)
                                   .atStartOfDay(ZoneId.systemDefault())
                                   .toInstant());
    }

    public static <T> List<T> addToList(final List<T> input, final T item) {
        val list = new ArrayList<>(Objects.requireNonNullElse(input, List.of()));
        list.add(item);
        return list;
    }

    public static <T> List<T> removeFromList(final List<T> input, final T item) {
        return Objects.requireNonNullElse(input, List.<T>of())
                .stream()
                .filter(t -> !t.equals(item))
                .toList();
    }


    public static List<String> aliasesForGroupingElements(List<GroupingElement> groupingElements) {
        return groupingElements.stream()
                .map(GroupingElement::getAlias)
                .toList();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T cloneObject(final T input) {
        try (val bos = new ByteArrayOutputStream(); val os = new ObjectOutputStream(bos)) {
            os.writeObject(input);
            os.flush();

            try (val ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
                return (T) ois.readObject();
            }
        }
    }

    public static Table<Integer, String, Object> tabulateTicketQueryResponse(
            TicketQueryResponse response,
            List<CQLEngine.SelectedField> selectedFields) {
        val output = TreeBasedTable.<Integer, String, Object>create();
        val rowIdx = new AtomicInteger(0);
        response.accept(new TicketQueryResponseVisitor<Void>() {
            @Override
            public Void visit(TicketListResponse listResponse) {
                listResponse.getResults()
                        .forEach(gist -> {
                            val cols = output.row(rowIdx.incrementAndGet());
                            cols.put(TicketGist.Fields.ticketId, gist.getTicketId());
                            cols.put(TicketGist.Fields.title, gist.getTitle());
                            cols.put(TicketGist.Fields.workflowName, gist.getWorkflowName());
                            cols.put(TicketGist.Fields.stateName, gist.getStateName());
                            cols.put(TicketGist.Fields.terminated, gist.isTerminated());
                            cols.put(TicketGist.Fields.priority, gist.getPriority());
                            cols.put(TicketGist.Fields.created, gist.getCreated());
                            cols.put(TicketGist.Fields.updated, gist.getUpdated());
                            val fieldsData = new HashMap<String, String>();
                            gist.getFields().forEach(field -> fieldsData.put(field.getFieldSchemaId(),
                                                                             ConductorServerUtils.toString(field.getFieldValue())));
                            selectedFields.forEach(selectedField -> cols.put(
                                    "fields_" + selectedField.name(),
                                    fieldsData.getOrDefault(selectedField.fieldSchemaId(), "")));
                        });
                return null;
            }

            @Override
            public Void visit(TicketGroupResponse groupResponse) {
                output.putAll(groupResponse.getCounts());
                return null;
            }

        });
        return output;
    }

    public static Table<Integer, String, Object> tabulateEventQueryResponse(
            EventQueryResponse response) {
        val output = TreeBasedTable.<Integer, String, Object>create();
        val rowIdx = new AtomicInteger(0);
        response.accept(new EventQueryResponseVisitor<Void>() {
            @Override
            public Void visit(EventListResponse listResponse) {
                listResponse.getResults()
                        .forEach(event -> {
                            val cols = output.row(rowIdx.incrementAndGet());
                            cols.put(Event.Fields.id, event.getId());
                            cols.put(Event.Fields.type, event.getType());
                            cols.put(Event.Fields.date, event.getDate().toInstant().toString());
                            cols.put(Event.Fields.objectType, event.getObjectType());
                            cols.put(Event.Fields.objectId, event.getObjectId());
                            cols.put(Event.Fields.userId, Strings.isNullOrEmpty(event.getUserId())
                                                          ? "" : event.getUserId());
                            val eventTime = event.getEventTime();
                            cols.put("date." + Event.EventTime.Fields.year, eventTime.getYear());
                            cols.put("date." + Event.EventTime.Fields.month, eventTime.getMonth());
                            cols.put("date." + Event.EventTime.Fields.day, eventTime.getDay());
                            cols.put("date." + Event.EventTime.Fields.hour, eventTime.getHour());
                            cols.put("date." + Event.EventTime.Fields.minute, eventTime.getMinute());
                            cols.put("date." + Event.EventTime.Fields.second, eventTime.getSecond());
                            cols.put("date." + Event.EventTime.Fields.millisecond, eventTime.getMillisecond());

                            //TODO::SUBCLASS FIELDS
                        });
                return null;
            }

            @Override
            public Void visit(EventGroupResponse groupResponse) {
                output.putAll(groupResponse.getCounts());
                return null;
            }

        });
        return output;
    }


    private static String toString(final FieldValue fieldValue) {
        return fieldValue.accept(new FieldValueVisitor<String>() {
            @Override
            public String visit(StringFieldValue stringFieldValue) {
                return stringFieldValue.getValue();
            }

            @Override
            public String visit(ChoiceFieldValue choiceFieldValue) {
                return String.join(",", choiceFieldValue.getValue());
            }

            @Override
            public String visit(BooleanFieldValue booleanFieldValue) {
                return Boolean.toString(booleanFieldValue.isValue());
            }

            @Override
            public String visit(NumberFieldValue numberFieldValue) {
                return Double.toString(numberFieldValue.getValue());
            }

            @Override
            public String visit(LocationFieldValue locationFieldValue) {
                return String.format("{%f,%f}", locationFieldValue.getLat(), locationFieldValue.getLon());
            }

            @Override
            public String visit(DateFieldValue dateFieldValue) {
                return new SimpleDateFormat("dd-MM-yyyy").format(dateFieldValue.getValue());
            }
        });
    }

    public static Date nextExecutionTimeForCron(String id, String cronExpression, Date currTime) {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        val parser = new CronParser(cronDefinition);
        val executionTime = ExecutionTime.forCron(parser.parse(cronExpression));
        return executionTime.nextExecution(ZonedDateTime.ofInstant(currTime.toInstant(),
                                                                   ZoneId.systemDefault()))
                .map(zonedDateTime -> Date.from(zonedDateTime.toInstant()))
                .orElseThrow(() -> new IllegalArgumentException("Could not determine next execution time for " + id));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> TreeBasedTable<Integer, String, Object> groupByAcrossShards(
            List<GroupingElement> groupingElements,
            Function<DetachedCriteria, Map<Integer, List>> generator,
            DetachedCriteria resultCriteria) {
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
                                                                                        timeBucketGroupingElement.getDateAttribute()));
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
                val divisor = divisorFromResolution(timeBucketGroupingElement);
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
        val queryResults = generator.apply(resultCriteria);
        val rows = queryResults.values()
                .stream()
                .map(list -> (List<Object[]>) list)
                .flatMap(List::stream)
                .toList();
        return parseGroupResponse(aliasedElements, rows);

    }

    private static int divisorFromResolution(TimeBucketGroupingElement timeBucketGroupingElement) {
        return switch (timeBucketGroupingElement.getResolution()) {
            case MINUTE -> 60;
            case HOUR -> 36_00;
            case DAY -> 864_00;
            case WEEK -> 7 * 864_00;
            case MONTH -> 30 * 864_00;
        };
    }

    private static TreeBasedTable<Integer, String, Object> parseGroupResponse(
            List<GroupingElement> groupingElements,
            List<Object[]> rows) {
        val output = new TreeMap<List<Object>, Long>((lhs, rhs) -> {
            //Comparator basically ensures sorting of the grouping keys
            val sizeComp = Integer.compare(lhs.size(), rhs.size());
            if(sizeComp != 0) {
                return sizeComp;
            }
            //List elements are either string or date
            for(var i = 0; i < lhs.size(); i++) {
                val lhsValue = lhs.get(i);
                val rhsValue = rhs.get(i);
                if(lhsValue instanceof String s) {
                    val res = s.compareTo((String) rhsValue);
                    if(0 != res) {
                        return res;
                    }
                }
                else if (lhsValue instanceof Date l) {
                    val res = l.equals(rhsValue)
                           ? 0
                            : (l.before((Date) rhsValue) ? -1 : 1);
                    if(res != 0) {
                        return res;
                    }
                }
            }
            return 0;
        });
        val formats = dateFormatsForTimeResolution();
        for (val row : rows) {
            val key = new ArrayList<>(row.length - 1);
            for (var colId = 0; colId < row.length - 1; colId++) {
                int finalColId = colId;
                key.add(groupingElements.get(colId)
                                .accept(new GroupingElementVisitor<>() {
                                    @Override
                                    public Object visit(ColumnGroupingElement columnGroupingElement) {
                                        return Objects.toString(row[finalColId]);
                                    }

                                    @Override
                                    public Object visit(TimeBucketGroupingElement timeBucketGroupingElement) {
                                        val divisor = divisorFromResolution(timeBucketGroupingElement);
                                        return toDate(row[finalColId], divisor);

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
                        val groupingElement = groupingElements.get(i);
                        val cellValue = keys.get(i);
                        groupingElement.accept(new GroupingElementVisitor<Void>() {
                            @Override
                            public Void visit(ColumnGroupingElement columnGroupingElement) {
                                row.put(columnGroupingElement.getAlias(), cellValue);
                                return null;
                            }

                            @Override
                            public Void visit(TimeBucketGroupingElement timeBucketGroupingElement) {
                                row.put(timeBucketGroupingElement.getAlias(),
                                        formats.get(timeBucketGroupingElement.getResolution())
                                                .format((Date) cellValue));
                                return null;
                            }
                        });
                    }
                    row.put("count", value);
                });
        /*val comparator =
                new Ordering<Table.Cell<Integer, String, Object>>() {
                    @Override
                    public int compare(
                            Table.Cell<Integer, String, Object> left,
                            Table.Cell<Integer, String, Object> right) {
                        return ;
                    }

                };*/
        return table;
    }

    public static Map<TimeResolution, SimpleDateFormat> dateFormatsForTimeResolution() {
        return EnumSet.allOf(TimeResolution.class)
                .stream()
                .collect(Collectors.toMap(Function.identity(), resolution -> switch (resolution) {
                    case MINUTE -> new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    case HOUR -> new SimpleDateFormat("yyyy-MM-dd HH");
                    case DAY -> new SimpleDateFormat("yyyy-MM-dd");
                    case WEEK -> new SimpleDateFormat("yyyy ww");
                    case MONTH -> new SimpleDateFormat("yyyy-MM");
                }));
    }

    @NonNull
    private static Date toDate(Object element, int divisor) {
        return new Date(1000L * divisor * (Long) element);
    }
}



