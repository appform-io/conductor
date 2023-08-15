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

import io.appform.conductor.model.utils.Displayable;
import lombok.Getter;

/**
 * Represents current state of a schema
 */
@Getter
public enum SchemaState implements Displayable {

    /**
     * Schema is active and ticket creation is allowed
     */
    ACTIVE("Active"),

    /**
     * Schema is inactive
     */
    INACTIVE("Inactive");

    private final String displayName;

    SchemaState(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String displayText() {
        return displayName;
    }
}
