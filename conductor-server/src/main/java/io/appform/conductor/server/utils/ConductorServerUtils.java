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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import io.appform.conductor.server.usermanagement.CurrentUserSessionStore;
import lombok.experimental.UtilityClass;

import java.util.Calendar;
import java.util.Date;

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

    public static String normalize(final String value) {
        return value
                .toLowerCase()
                .replaceAll("\\p{Punct}","_");
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
}
