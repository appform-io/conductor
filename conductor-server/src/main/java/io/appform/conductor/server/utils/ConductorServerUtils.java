/*
 * Copyright (c) 2023 Santanu Sinha
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.Schema;
import io.appform.conductor.model.ticket.TicketDetails;
import io.appform.conductor.model.ticket.fields.FieldValueVisitor;
import io.appform.conductor.model.ticket.fields.TicketField;
import io.appform.conductor.model.ticket.fields.impl.*;
import io.appform.conductor.server.usermanagement.CurrentUserSessionStore;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                .replaceAll("\\p{Punct}","_");
    }
    public static String upperSnake(final String value) {
        return value
                .trim()
                .toUpperCase()
                .replaceAll("\\p{Space}", "_")
                .replaceAll("\\p{Punct}","_");
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static void ensure(boolean condition, ConductorErrorCode errorCode, String message) {
        if(!condition) {
            throw new ConductorException(errorCode, Map.of("message", message), null);
        }
    }

    public static void notNull(Object object, ConductorErrorCode errorCode, String message) {
        if(null == object) {
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

    public static void ensureNonNull(@Nullable final Object testObject, ConductorErrorCode errorCode, Map<String, Object> context) {
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
                .forEach(field -> fields.set(fieldSchemaMap.get(field.getFieldSchemaId()).getName(),
                                             mapValueToJsonNode(mapper, field)));
        return node;
    }

    public static JsonNode mapValueToJsonNode(ObjectMapper mapper, TicketField field) {
        return field.getFieldValue().accept(new FieldValueVisitor<>() {
            @Override
            public JsonNode visit(StringFieldValue stringFieldValue) {
                return mapper.createObjectNode()
                        .textNode(stringFieldValue.getValue());
            }

            @Override
            public JsonNode visit(ChoiceFieldValue choiceFieldValue) {
                val choices = mapper.createArrayNode();
                Objects.requireNonNullElse(choiceFieldValue.getValue(), List.<String>of())
                        .forEach(choices::add);
                return choices;
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
        val connectionTimeout = Duration.ofSeconds(2);
        val connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(100);
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(connectionTimeout))
                .setSocketTimeout(Timeout.of(connectionTimeout))
                .setValidateAfterInactivity(TimeValue.ofSeconds(10))
                .setTimeToLive(TimeValue.ofHours(1))
                .build());
        val requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(connectionTimeout))
                .setResponseTimeout(Timeout.of(connectionTimeout))
                .build();
        return HttpClients.custom()
                .disableRedirectHandling()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

}
