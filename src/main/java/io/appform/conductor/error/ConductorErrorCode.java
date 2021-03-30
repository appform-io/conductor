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

package io.appform.conductor.error;

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
    ;

    private final int errorCode;
    private final String messageFormat;

    ConductorErrorCode(int errorCode, String messageFormat) {
        this.errorCode = errorCode;
        this.messageFormat = messageFormat;
    }
}
