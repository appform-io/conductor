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

package io.appform.conductor.server.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.internal.lang3.math.NumberUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.attributes.AttributeType;
import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.attributes.definition.impl.ChoiceAttributeDefinition;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.schema.fields.ChoiceFieldSchema;
import io.appform.conductor.model.utils.Displayable;
import io.appform.conductor.server.utils.StringUtils;
import io.appform.conductor.server.utils.dev.IgnoreGenerated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.text.WordUtils;

import org.jsoup.Jsoup;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 *
 */
@Slf4j
@IgnoreGenerated
@SuppressWarnings("unused")
public class CustomHelpers {

    private static final String POINTER = "pointer";
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_TIME_ZONE = "Asia/Calcutta";
    private static final String DEFAULT_DATE_FORMAT = "dd-MM-yyyy";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
    private static final Clock CLOCK = Clock.system(ZoneId.of(DEFAULT_TIME_ZONE));
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode NULL_NODE = NullNode.getInstance();


    public String readable(Object input) {
        if (Objects.isNull(input)) {
            return "";
        }

        if (input instanceof Displayable displayable) {
            return displayable.displayText();
        }
        return WordUtils.capitalizeFully(input.toString().replace('_', ' '));
    }

    @SneakyThrows
    public CharSequence emptyStr(String input, Options options) {
        return Strings.isNullOrEmpty(input)
               ? options.fn()
               : options.inverse();
    }

    @SneakyThrows
    public CharSequence empty(Object input, Options options) {
        return Objects.isNull(input)
                       || (input instanceof Collection<?> c && c.isEmpty())
                       || (input instanceof String s && s.isEmpty())
                       || (input instanceof Map<?, ?> m && m.isEmpty())
               ? options.fn()
               : options.inverse();
    }

    @SneakyThrows
    public <T> CharSequence contains(Collection<T> collection, T key, Options options) {
        return (null != collection && collection.contains(key))
               ? options.fn()
               : options.inverse();
    }

    @SneakyThrows
    public CharSequence hasKey(Map<String, Boolean> collection, String key, Options options) {
        return collection.containsKey(key)
               ? options.fn()
               : options.inverse();
    }

    @SneakyThrows
    public CharSequence fieldTypeEquals(FieldType lhs, String value, Options options) {
        return FieldType.valueOf(value).equals(lhs)
               ? options.fn()
               : options.inverse();
    }

    @SneakyThrows
    public CharSequence attrTypeEquals(AttributeType lhs, String value, Options options) {
        return AttributeType.valueOf(value).equals(lhs)
               ? options.fn()
               : options.inverse();
    }

    @SneakyThrows
    public <E extends Enum<E>> CharSequence eqEnum(final E lhs, final Object rhs, Options options) {
        return lhs != null && lhs.name().equals(rhs)
               ? options.fn()
               : options.inverse();
    }

    @SneakyThrows
    public CharSequence htmlDate(Date date, boolean time) {
        return null == date ? "1970-01-01" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    @SneakyThrows
    public CharSequence htmlDateTime(Date date) {
        return null == date ? "1970-01-01 00:00:00" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    public Object map(Map<String, Object> map, String key) {
        return map.get(key);
    }

    public Object join(Collection<Object> collection) {
        return null == collection ? "" : Joiner.on(",").join(collection);
    }

    public CharSequence choiceValues(final FieldSchema choiceFieldSchema) {
        if(null == choiceFieldSchema || choiceFieldSchema.getType() != FieldType.CHOICE) {
            return null;
        }
        return ((ChoiceFieldSchema)choiceFieldSchema).getChoices()
                .stream()
                .map(ChoiceFieldSchema.Option::getValue)
                .collect(Collectors.joining(","));
    }

    public CharSequence choiceAttrValues(final AttributeDefinition choiceAttrDef) {
        if(null == choiceAttrDef || choiceAttrDef.getType() != AttributeType.CHOICE) {
            return null;
        }
        return Joiner.on(",").join(((ChoiceAttributeDefinition)choiceAttrDef).getOptions());
    }

    public String actionFragment(final ActionType type) {
        return switch (type) {
            case WEBHOOK -> "actions/fragments/webhook.hbs";
            case ROUTE_TO_GROUP -> "actions/fragments/add-to-group.hbs";
            case ADD_COMMENT -> "actions/fragments/add-comment.hbs";
            case ADD_TICKET_ACTION -> "actions/fragments/add-ticket.hbs";
            case CHANGE_PRIORITY -> "actions/fragments/change-priority.hbs";
            case SET_FIELD -> "actions/fragments/set-field.hbs";
        };
    }

    public String minutes(final Duration duration) {
        if(null == duration) {
            return null;
        }
        return Objects.toString(duration.toMinutes());
    }

    public long currTime() {
        return System.currentTimeMillis();
    }

    @SneakyThrows
    public long elapsedTime(String dateFormat, String fromDateStr, String toDateStr) {
        val sdf = new SimpleDateFormat(dateFormat);
        var toDate = new Date();
        Date fromDate = null;
        if (null == fromDateStr) {
            log.error("From date could not be extracted from {}", fromDateStr);
            return 0;
        }
        try {
            fromDate = sdf.parse(fromDateStr);
        } catch (ParseException e) {
            log.error("Error parsing from date: " + fromDateStr, e);
            return 0;
        }
        if (null != toDateStr) {
            try {
                toDate = sdf.parse(toDateStr);
            } catch (ParseException e) {
                log.error("Error parsing to date: " + toDateStr, e);
            }
        }
        if (null == toDate) {
            toDate = new Date();
        }
        return toDate.getTime() - fromDate.getTime();
    }


    public boolean streq(String value, String compareWith) {
        return !Strings.isNullOrEmpty(value) && value.equals(compareWith);
    }

    public boolean streqi(String value, String compareWith) {
        return !Strings.isNullOrEmpty(value) && value.equalsIgnoreCase(compareWith);
    }

    public long add(Number aNumber, Number bNumber) {
        return aNumber.longValue() + bNumber.longValue();
    }

    public Double addDouble(Number aNumber, Number bNumber) {
        return aNumber.doubleValue() + bNumber.doubleValue();
    }

    /*
     * computes no. of days from the given date till the current date.
     * {{computeDays dateNode 'dd-MM-yyyy'}}
     */
    public int computeDays(String context, String inDateFormat, String inTimeZone) {
        if (null == context || Strings.isNullOrEmpty(context)) {
            return 0;
        }
        val dateFormat = Strings.isNullOrEmpty(inDateFormat) ? DEFAULT_DATE_FORMAT : inDateFormat;
        val timeZone = Strings.isNullOrEmpty(inTimeZone) ? DEFAULT_TIME_ZONE : inTimeZone;
        long epochTimestamp = getEpochTimestamp(context, dateFormat, timeZone);
        val currentTime = ZonedDateTime.now(CLOCK).withZoneSameInstant(ZoneId.of(timeZone));
        val diff = currentTime.toInstant().toEpochMilli() - epochTimestamp;
        if (diff < 0) {
            return 0;
        }
        return Math.round(diff / 86_400_000);
    }


    public String countIf(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String pointers = options.hash("pointers");
        if (Strings.isNullOrEmpty(pointers)) {
            return "0";
        }
        val keys = pointers.split(",");
        if (keys.length == 0) {
            return "0";
        }
        val count = Arrays.stream(keys)
                .filter(keyElement -> {
                    val key = keyElement.trim();
                    final int lastIndex = lastIndex(options);
                    int value = lastIndex;
                    if (!Strings.isNullOrEmpty(key)) {
                        value = readString(node, key, lastIndex);
                    }
                    return options.hash("op_" + value);
                })
                .count();
        return Long.toString(count);
    }

    public String countMatchStr(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String pointers = options.hash(POINTER);
        if (Strings.isNullOrEmpty(pointers)) {
            return "0";
        }
        val keys = pointers.split(",");
        if (keys.length == 0) {
            return "0";
        }
        val count = Arrays.stream(keys)
                .filter(keyElement -> {
                    String value = EMPTY_STRING;
                    val key = keyElement.trim();
                    if (!Strings.isNullOrEmpty(key)) {
                        val dataNode = node.at(key);
                        if (!(null == dataNode || dataNode.isNull() || dataNode.isMissingNode() || !dataNode.isValueNode())) {
                            value = dataNode.asText();
                        }
                    }
                    return options.hash(normalizedKey(value), false);
                })
                .count();
        return Long.toString(count);
    }

    public String alphaNumeric(String value) {
        return StringUtils.alphaNumeric(value);
    }

    public String normalize(String value) {
        return StringUtils.normalize(value);
    }

    public String singleLineText(String value) {
        return StringUtils.removeNewLineAndTabs(value);
    }

    public String normalizeUpper(String value) {
        return StringUtils.normalize(value).toUpperCase();
    }

    public String normalizeInitCap(String value) {
        return StringUtils.normalizeInitCap(value);
    }

    public String dateFormat(Long context, String dateFormatStr, String inTimeZone) {
        try {
            if (null != context) {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormatStr);
                String timeZone = inTimeZone == null
                        ? DEFAULT_TIME_ZONE
                        : inTimeZone;
                sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
                return sdf.format(context);
            }
        } catch (Exception e) {
            log.error("Error formatting date", e);
        }
        return null;
    }

    public long toEpochTime(String context, String dateFormatStr, String inTimeZone) {
        try {
            if (null != context) {
                String timeZone = inTimeZone == null
                        ? DEFAULT_TIME_ZONE
                        : inTimeZone;
                return getEpochTimestamp(context, dateFormatStr, timeZone);
            }
        } catch (Exception e) {
            log.error("Error formatting date", e);
        }
        return 0L;
    }

    public String toLowerCase(String context) {
        if (!Strings.isNullOrEmpty(context)) {
            return context.toLowerCase().trim();
        }
        return null;
    }

    public String toUpperCase(String context) {
        if (!Strings.isNullOrEmpty(context)) {
            return context.toUpperCase().trim();
        }
        return null;
    }

    public String rsub(String context, Integer index) {
        if (!Strings.isNullOrEmpty(context)) {
            int length = context.length();
            if (length >= index) {
                return context.substring(length - index);
            }
            return context;
        }
        return null;
    }

    public String date(Long context) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        try {
            if (null != context) {
                return sdf.format(new Date(context));
            }
        } catch (Exception e) {
            log.error("Error converting date", e);
        }
        return sdf.format(new Date());
    }

    public String incDate(Integer context) {
        val amountToAdd = context != null ? context : 1;
        return SIMPLE_DATE_FORMAT.format(Date.from(Instant.now().plus(amountToAdd, ChronoUnit.DAYS)).getTime());
    }

    public String numberTostr(Number aNumber) {
        return String.valueOf(aNumber.doubleValue());
    }

    @SneakyThrows
    public CharSequence mapLookup(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String key = options.hash(POINTER);
        final int lastIndex = lastIndex(options);
        int value = lastIndex;
        if (!Strings.isNullOrEmpty(key)) {
            value = readString(node, key, lastIndex);
        }
        return singleElement(options, value);
    }

    @SneakyThrows
    public String mapLookupArr(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String key = options.hash(POINTER);
        final int lastIndex = lastIndex(options);
        List<Integer> indices = new ArrayList<>();
        if (Strings.isNullOrEmpty(key)) {
            log.warn("Invalid json node. Defaulting to array of last index: {} for empty key", lastIndex);
            indices.add(lastIndex);
        } else {
            val keyNode = node.at(key);
            if (!keyNode.isArray()) {
                readNode(lastIndex, indices, keyNode);
            } else {
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                keyNode.elements(), Spliterator.ORDERED), false)
                        .forEach(childNode -> readNode(lastIndex, indices, childNode));
            }
        }                //Array of options
        return MAPPER.writeValueAsString(
                indices.stream()
                        .map(i -> options.hash("op_" + i))
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()));
    }

    /**
     * Example Usage:
     * JsonNode obj <- {"state" : "karnataka","language" : 2 }
     * defaults to english under error cases
     * Template template = handlebars.compileInline("{{mapArrLookup array='english,hindi,tamil,bengali,kannada' op_karnataka='2,3,1' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}" );
     */
    @SneakyThrows
    public String mapArrLookup(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String key = options.hash("array");
        if (Strings.isNullOrEmpty(key)) {
            return EMPTY_STRING;
        }

        String[] res = key.split(",");
        if (res.length == 1) {
            return res[0];
        }

        final String key1 = options.hash("key1");
        val key1Data = node.at(key1);
        if (null == key1Data || key1Data.isNull() || !key1Data.isTextual()) {
            return res[0];
        }
        String key1String = key1Data.asText();

        final String key2 = options.hash("key2");
        val key2Value = node.at(key2);
        if (null == key2Value || key2Value.isNull()) {
            return res[0];
        }

        final String arrayOrderCsv = options.hash("op_" + key1String);
        if (null == arrayOrderCsv || arrayOrderCsv.isEmpty()) {
            return res[0];
        }

        String[] arrayOrder = arrayOrderCsv.split(",");
        if (key2Value.asInt() > 0 && key2Value.asInt() <= arrayOrder.length) {
            String arrayLookupIndexStr = arrayOrder[key2Value.asInt() - 1];
            int arrayLookupIndex = Integer.parseInt(arrayLookupIndexStr);
            if (arrayLookupIndex > 1 && arrayLookupIndex <= res.length) {
                return (res[arrayLookupIndex - 1]);
            }
        }

        return res.length > 0 ? res[0] : EMPTY_STRING;
    }

    /**
     * Example Usage:
     * JsonNode obj <- {"state" : "KA","language" : 41 }
     * defaults to english under error cases
     * Template template = handlebars.compileInline("{{multiMapLookup default='english' op_ka='1:kannada,2:hindi,3:english,41:tamil,42:telugu,43:malayalam' op_tn='3:tamil' key1='/state' key2='/language'}}" );
     */
    public String multiMapLookup(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        String defFeched = options.hash("default");
        final String defaultValue = (null == defFeched || defFeched.isEmpty()) ? EMPTY_STRING
                : defFeched;

        final String key1 = options.hash("key1");
        if (null == key1 || key1.isEmpty()) return defaultValue;
        val key1Data = node.at(key1);
        if (null == key1Data || key1Data.isNull() || !key1Data.isTextual()) return defaultValue;
        String key1String = normalizedKey(key1Data.asText());

        final String key2 = options.hash("key2");
        if (null == key2 || key2.isEmpty()) return defaultValue;
        val key2Data = node.at(key2);
        if (null == key2Data || key2Data.isNull()) return defaultValue;
        String key2String = key2Data.asText();

        Map<String, String> kvMap = Maps.newHashMap();
        {
            final String kvPairs = options.hash(key1String);
            if (null == kvPairs || kvPairs.isEmpty()) return defaultValue;

            String[] pairs = kvPairs.split(",");

            for (String pair : pairs) {
                String[] tuple = pair.split(":");
                if (tuple.length == 2) {
                    kvMap.put(tuple[0], tuple[1]);
                }
            }
        }

        if (kvMap.containsKey(key2String)) {
            return kvMap.get(key2String);
        }

        return defaultValue;
    }

    @SneakyThrows
    public String translate(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String key = options.hash(POINTER);
        if (Strings.isNullOrEmpty(key)) {
            return MAPPER.writeValueAsString(NULL_NODE);
        }
        val dataNode = node.at(key);
        if (null == dataNode || dataNode.isNull() || dataNode.isMissingNode() || !dataNode.isValueNode()) {
            return MAPPER.writeValueAsString(NULL_NODE);
        }
        val lookupKey = dataNode.asText();
        if (Strings.isNullOrEmpty(lookupKey)) {
            return MAPPER.writeValueAsString(NULL_NODE);
        }
        val lookupValue = options.hash(normalizedKey(lookupKey));
        if (null == lookupValue) {
            return MAPPER.writeValueAsString(NULL_NODE);
        }
        return MAPPER.writeValueAsString(lookupValue);
    }

    @SneakyThrows
    public String translateArr(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String key = options.hash(POINTER);
        if (Strings.isNullOrEmpty(key)) {
            return EMPTY_STRING;
        }
        val dataNode = node.at(key);
        if (null == dataNode || dataNode.isNull() || dataNode.isMissingNode()
                || (!dataNode.isValueNode() && !dataNode.isArray())) {
            return EMPTY_STRING;
        }
        val lookupKeys = new ArrayList<String>();
        if (dataNode.isValueNode()) {
            val lookupKey = dataNode.asText();
            if (Strings.isNullOrEmpty(lookupKey)) {
                return EMPTY_STRING;
            }
            lookupKeys.add(lookupKey);
        } else if (dataNode.isArray()) {
            lookupKeys.addAll(StreamSupport.stream(Spliterators.spliteratorUnknownSize(dataNode.elements(),
                                    Spliterator.ORDERED),
                            false)
                    .filter(child -> !child.isNull() && !child.isMissingNode() && child.isValueNode())
                    .map(JsonNode::asText)
                    .collect(Collectors.toList()));

        }
        return MAPPER.writeValueAsString(lookupKeys.stream()
                .map(lookupKey -> {
                    val value = options.hash(normalizedKey(lookupKey));
                    return null == value
                            ? NullNode.getInstance()
                            : value;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @SneakyThrows
    public String translateTxt(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String key = options.hash(POINTER);
        if (Strings.isNullOrEmpty(key)) {
            return EMPTY_STRING;
        }
        val dataNode = node.at(key);
        if (null == dataNode || dataNode.isNull() || dataNode.isMissingNode() || !dataNode.isValueNode()) {
            return EMPTY_STRING;
        }
        val lookupKey = dataNode.asText();
        if (Strings.isNullOrEmpty(lookupKey)) {
            return EMPTY_STRING;
        }
        val lookupValue = options.hash(normalizedKey(lookupKey));
        if (null == lookupValue) {
            return EMPTY_STRING;
        }
        return lookupValue.toString();
    }

    @SneakyThrows
    public String translateArrTxt(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String key = options.hash(POINTER);
        if (Strings.isNullOrEmpty(key)) {
            return EMPTY_STRING;
        }
        val dataNode = node.at(key);
        if (null == dataNode || dataNode.isNull() || dataNode.isMissingNode()
                || (!dataNode.isValueNode() && !dataNode.isArray())) {
            return EMPTY_STRING;
        }
        val lookupKeys = new ArrayList<String>();
        if (dataNode.isValueNode()) {
            val lookupKey = dataNode.asText();
            if (Strings.isNullOrEmpty(lookupKey)) {
                return EMPTY_STRING;
            }
            lookupKeys.add(lookupKey);
        } else if (dataNode.isArray()) {
            lookupKeys.addAll(StreamSupport.stream(Spliterators.spliteratorUnknownSize(dataNode.elements(),
                                    Spliterator.ORDERED),
                            false)
                    .filter(child -> !child.isNull() && !child.isMissingNode() && child.isValueNode())
                    .map(JsonNode::asText)
                    .collect(Collectors.toList()));

        }
        return lookupKeys.stream()
                .map(lookupKey -> {
                    val value = options.hash(normalizedKey(lookupKey));
                    return null == value
                            ? null
                            : value.toString();
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    @SneakyThrows
    public String trimDecimalPointsPtr(Object context, Options options) {
            try {
                val node = MAPPER.valueToTree(context);
                final String pointer = options.hash(POINTER);
                if (!Strings.isNullOrEmpty(pointer) && null != node && !node.isNull() && !node.isMissingNode()) {
                    JsonNode contextNode = node.at(pointer);
                    String c =
                            contextNode == null || contextNode.isMissingNode() || contextNode.isNull()
                                    ? null : contextNode.asText();
                    return trimDecimalPoints(c);
                }
            } catch (Exception e) {
                log.error("Exception occurred while removing decimal points", e);
            }
            return "-1";
    }

    public String trimDecimalPoints(String context) {
        if (!Strings.isNullOrEmpty(context)) {
            if (context.contains(".")) {
                return context.substring(0, context.indexOf("."));
            } else {
                return context;
            }
        }
        return "-1";
    }
    @SneakyThrows
    public String valueFromJsonString(String jsonString, Options options) {
        try {
            val pointer = (String) options.hash(POINTER);
            if (!Strings.isNullOrEmpty(pointer) && !Strings.isNullOrEmpty(jsonString)) {
                val node = MAPPER.readTree(jsonString);
                val valueNode = node.at(pointer);
                if (Objects.isNull(valueNode) || valueNode.isNull()) {
                    return "";
                } else {
                    return valueNode.asText();
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred while removing decimal points", e);
        }
        return "";
    }

    public long toIntPtr(Object context, Options options) {
        val node = MAPPER.valueToTree(context);
        final String pointer = options.hash(POINTER);
        if (!Strings.isNullOrEmpty(pointer) && null != node && !node.isNull() && !node.isMissingNode()) {
            val intNode = node.at(pointer);
            if (intNode.isIntegralNumber()) {
                return intNode.asLong();
            }
            if (intNode.isTextual()) {
                try {
                    return Integer.parseInt(numericStr(intNode.asText()));
                } catch (NumberFormatException e) {
                    log.error("Count not parse value: {} at: {}", node, pointer);
                }
            }
        }
        return -1;
    }

    @SneakyThrows
    public Object selectValuesGivenRange(Number aNumber, Options options) {
            val optionFrom = options.param(0);
            val optionTo = options.param(1);
            val optionTrue = options.param(2);
            val optionFalse = options.param(3);

            if ( aNumber == null ) return optionFalse;

            if (optionFrom instanceof Long && optionTo instanceof Long) {
                return (Long.compare(aNumber.longValue(), (Long) optionFrom) > 0 && Long.compare((Long) optionTo, aNumber.longValue()) > 0) ? optionTrue : optionFalse;
            }
            if (optionFrom instanceof Integer && optionTo instanceof Integer) {
                return (Integer.compare(aNumber.intValue(), (Integer) optionFrom) > 0 && Integer.compare((Integer) optionTo, aNumber.intValue()) > 0) ? optionTrue : optionFalse;
            }
            if (optionFrom instanceof Double && optionTo instanceof Double) {
                return (Double.compare(aNumber.doubleValue(), (Double) optionFrom) > 0 && Double.compare((Double) optionTo, aNumber.doubleValue()) > 0) ? optionTrue : optionFalse;
            }
            if (optionFrom instanceof String && optionTo instanceof String) {
                return (Double.compare(aNumber.doubleValue(), Double.valueOf((String) optionFrom)) > 0 && Double.compare(Double.valueOf((String) optionTo), aNumber.doubleValue()) > 0) ? optionTrue : optionFalse;
            }
            return null;
    }

    public String phone(String context) {
        if (Strings.isNullOrEmpty(context)) {
            return null;
        }
        final String numericString = numericStr(context);
        if (Strings.isNullOrEmpty(numericString)) {
            return null;
        }
        if (numericString.length() == 10) {
            return numericString;
        }
        return numericString.length() > 10
                ? numericString.substring(numericString.length() - 10)
                : null;
    }

    @SneakyThrows
    public boolean emptyNode(Object context) {
        if(context == null) {
            return true;
        }
        val node = MAPPER.valueToTree(context);
        if (node.isNull() || node.isMissingNode()) {
            return true;
        }
        if (node.isEmpty()) {
            return true;
        }

        if (node.isArray()) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.elements(),
                            Spliterator.ORDERED), false)
                    .allMatch(n -> n.isNull() || n.isMissingNode() || (n.isTextual() && Strings.isNullOrEmpty(
                            n.asText())));
        }
        return false;
    }

    @SneakyThrows
     public boolean notEmptyNode(Object context) {
        if(context == null) {
            return false;
        }
        val node = MAPPER.valueToTree(context);
        if (node.isNull() || node.isMissingNode()) {
            return false;
        }
        if (node.isEmpty()) {
            return false;
        }

        if (node.isArray()) {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.elements(),
                            Spliterator.ORDERED), false)
                    .noneMatch(n -> n.isNull() || n.isMissingNode() || (n.isTextual() && Strings.isNullOrEmpty(
                            n.asText())));
        }
        return true;
    }

    @SneakyThrows
    public int toInt(String context) {
        if (!Strings.isNullOrEmpty(context)) {
            try {
                return Integer.parseInt(numericStr(context));
            } catch (NumberFormatException e) {
                log.error("Count not parse string value: {}", context);
            }
        }
        return -1;
    }

    @SneakyThrows
    public String URLEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public String localTime(String timeZone) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timeZone));
        return MAPPER.writeValueAsString(MAPPER.createObjectNode()
                .set("localTime", MAPPER.createObjectNode()
                        .put("hour", now.getHour())
                        .put("minutes", now.getMinute())
                        .put("seconds", now.getSecond())));
    }

    public boolean isDigits(String str) {
        return NumberUtils.isDigits(str);
    }

    public String sanitizeJson(String str) {
        if (str == null) {
            return NullNode.getInstance().toString();
        } else {
            return TextNode.valueOf(str).toString();
        }
    }

    @SneakyThrows
    public String html2Text(String htmlString, Options options) {
                final String text = Jsoup.parse(htmlString).text();
                return Strings.isNullOrEmpty(text)
                        ? EMPTY_STRING
                        : text.replace("\\n", EMPTY_STRING).trim();
    }

    private int extractOptionValue(JsonNode keyNode, int defaultValue) {
        final String text = keyNode.asText();
        try {
            return Strings.isNullOrEmpty(text)
                    ? defaultValue
                    : Integer.parseInt(numericStr(text));
        } catch (NumberFormatException e) {
            log.error("Error parsing number text: " + text, e);
            return defaultValue;
        }
    }


    private int readString(JsonNode node, String key, int lastIndex) {
        int value = lastIndex;
        val keyNode = node.at(key);
        if (keyNode.isTextual()) {
            value = extractOptionValue(keyNode, lastIndex);
        }
        if (keyNode.isIntegralNumber()) {
            value = keyNode.asInt(lastIndex);
        }
        return Math.min(value, lastIndex);
    }

    private void readNode(int lastIndex, List<Integer> indices, JsonNode keyNode) {
        int value;
        if (keyNode.isTextual()) {
            value = extractOptionValue(keyNode, lastIndex);
        } else if (keyNode.isIntegralNumber()) {
            value = keyNode.asInt(lastIndex);
        } else {
            value = lastIndex;
        }
        if (value < 10) {
            //Single option -> return one value
            indices.add(Math.min(value, lastIndex));
        } else {
            //Multiple concatenated options
            List<Integer> revIndices = new ArrayList<>();
            while (value > 0) {
                revIndices.add(Math.min(value % 10, lastIndex));
                value = value / 10;
            }
            Collections.reverse(revIndices);
            indices.addAll(revIndices);
        }
    }

    @SneakyThrows
    private CharSequence singleElement(Options options, int value) {
        return MAPPER.writeValueAsString(options.hash("op_" + value));
    }

    private String numericStr(String context) {
        return context.replaceAll("[^\\p{Digit}]", EMPTY_STRING);
    }

    private int lastIndex(Options options) {
        return options.hash.size() - 1;
    }

    private String normalizedKey(String s) {
        return "op_" + StringUtils.normalize(s);
    }

    private long getEpochTimestamp(String date, String dateFormat, String timeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        try {
            return sdf.parse(date).getTime();
        } catch (ParseException e) {
            log.error("Error parsing to date: " + date, e);
        }
        return 0L;
    }

}
