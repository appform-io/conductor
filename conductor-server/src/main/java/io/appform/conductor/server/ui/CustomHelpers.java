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

import com.github.jknack.handlebars.Options;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import io.appform.conductor.model.actions.ActionType;
import io.appform.conductor.model.schema.FieldSchema;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.schema.fields.ChoiceFieldSchema;
import io.appform.conductor.model.utils.Displayable;
import io.appform.conductor.server.utils.dev.IgnoreGenerated;
import lombok.SneakyThrows;
import org.apache.commons.text.WordUtils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 */
@IgnoreGenerated
@SuppressWarnings("unused")
public class CustomHelpers {
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

}
