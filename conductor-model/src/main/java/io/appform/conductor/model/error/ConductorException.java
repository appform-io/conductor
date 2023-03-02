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

import lombok.Builder;
import lombok.Getter;
import lombok.val;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;

/**
 * Global exception object
 */
@Getter
public class ConductorException extends RuntimeException {
    private final ConductorErrorCode errorCode;
    private final transient Map<String, Object> context;

    @Builder
    public ConductorException(
            ConductorErrorCode errorCode,
            Map<String, Object> context,
            Throwable cause) {
        super(generateErrorMessage(errorCode, context, cause), cause);
        this.errorCode = errorCode;
        this.context = context;
    }

    private static String generateErrorMessage(ConductorErrorCode errorCode, Map<String, Object> context, Throwable cause) {
        val coreMessage = StringSubstitutor.replace(errorCode.getMessageFormat(), context);
        return null != cause
                ? coreMessage + " Root Cause: " + cause.getMessage()
                : coreMessage;
    }
}
