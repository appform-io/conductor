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
import com.google.common.base.Strings;
import io.appform.conductor.model.schema.FieldType;
import io.appform.conductor.model.utils.Displayable;
import io.appform.conductor.server.utils.dev.IgnoreGenerated;
import lombok.SneakyThrows;
import org.apache.commons.text.WordUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
@IgnoreGenerated
@SuppressWarnings("unused")
public class CustomHelpers {
    public String readable(Object input) {
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
    public CharSequence htmlDate(Date date, boolean time) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    public Object map(Map<String, Object> map, String key) {
        return map.get(key);
    }

}
