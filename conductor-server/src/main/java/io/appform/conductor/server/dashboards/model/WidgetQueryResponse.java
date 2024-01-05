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

package io.appform.conductor.server.dashboards.model;

import lombok.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class WidgetQueryResponse {
    public enum Type {
        BAR,
        TIME_SERIES,
        PIE,
        FLARE
    }

    public record DataSetElement(String label, //Group name
                                 List<Object> data) {} //Y axis values

    Type type;
    @Singular
    Collection<String> labels; //X axis
    @Singular
    List<DataSetElement> datasets;
    Map<String, Object> extraMeta;
}
