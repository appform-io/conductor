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

package io.appform.conductor.model.attributes;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;

/**
 *
 */
@FieldNameConstants
@Getter
public enum AttributeType {
    STRING(Values.STRING_TYPE),
    CHOICE(Values.CHOICE_TYPE),
    NUMBER(Values.NUMBER_TYPE),
    DATE(Values.DATE_TYPE),
    LINK(Values.LINK_TYPE),
    ;

    private final String value;

    AttributeType(String value) {
        this.value = value;
    }

    public static final class Values {
        public static final String STRING_TYPE="STRING";
        public static final String CHOICE_TYPE="CHOICE";
        public static final String NUMBER_TYPE="NUMBER";
        public static final String DATE_TYPE="DATE";
        public static final String LINK_TYPE="LINK";
    }
}
