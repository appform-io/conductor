/*
 * Copyright (c) 2021 Santanu Sinha
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

package io.appform.conductor.model.schema;

import lombok.Getter;

/**
 * Type for the field. This will govern the Field's behaviour, validations, rendering etc
 */
public enum FieldType {

    /**
     * String field type.
     */
    STRING("String"),

    /**
     * Field consists of a choice of fixed values
     */
    CHOICE("Choice"),

    /**
     * Boolean field type
     */
    BOOLEAN("Boolean"),

    /**
     * Number field type. Will be represented as double
     */
    NUMBER("Number"),

    /**
     * Location field type. Will store latitude and longitude
     */
    LOCATION("Location"),

    /**
     * Date field type. Will store value as epoch.
     */
    DATE("Date")
    ;

    public static final String STRING_TEXT = "STRING";
    public static final String CHOICE_TEXT = "CHOICE";
    public static final String BOOLEAN_TEXT = "BOOLEAN";
    public static final String NUMBER_TEXT = "NUMBER";
    public static final String LOCATION_TEXT = "LOCATION";
    public static final String DATE_TEXT = "DATE";

    /**
     * Display name
     */
    @Getter
    private final String displayName;

    FieldType(String displayName) {
        this.displayName = displayName;
    }

}
