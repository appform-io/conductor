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

package io.appform.conductor.model.error;

import lombok.Getter;

/**
 * Global error table
 */
@Getter
public enum ConductorErrorCode {
    STORE_READ_ERROR(1001, "Error reading database for type ${type} with ${id}"),
    STORE_WRITE_ERROR(1002, "Error saving to database for type ${type} with ${id}"),
    STORE_UPDATE_ERROR(1003, "Error updating database for type ${type} with id ${id}"),
    STORE_LIST_ERROR(1004, "Error reading object list of type ${type}"),
    STORE_QUERY_ERROR(1005, "Error reading object of type ${type} using query of param type ${paramType} and value ${value}"),
    STORE_RELATED_ENTITY_READ_ERROR(1006, "Error reading database for type ${type} with ${id}/${subId}"),
    STORE_RELATED_ENTITY_WRITE_ERROR(1007, "Error saving to database for type ${type} with ${id}/${subId}"),
    STORE_RELATED_ENTITY_UPDATE_ERROR(1008, "Error updating database for type ${type} with id ${id}/${subId}"),


    SCHEMA_UPDATE_FAILED(2001, "Error updating schema version ${schemaId}/${version}. Operation attempted: ${operation}"),
    SCHEMA_FIELD_WRITE_FAILED(2002, "Error writing schema field for ${schemaId}/${fieldId}"),
    SCHEMA_FIELD_READ_FAILED(2003, "Error reading schema field for ${schemaId}/${fieldId}"),


    ;

    private final int errorCode;
    private final String messageFormat;

    ConductorErrorCode(int errorCode, String messageFormat) {
        this.errorCode = errorCode;
        this.messageFormat = messageFormat;
    }
}
