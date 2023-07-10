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

package io.appform.conductor.model.actions;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;

/**
 * An action that can be taken as a transition
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Action {


    /**
     * Type of action
     */
    private final ActionType type;

    /**
     * Global ID for the action
     */
    private String id;

    /**
     * Human-readable name for the action
     */
    private String name;

    /**
     * Human-readable description of what the action does
     */
    private String description;

    /**
     * Date when action was created
     */
    private Date created;

    /**
     * Date when action was last updated
     */
    private Date updated;

    /**
     * Accept and execute an implementation of {@link ActionVisitor} to operate on subtypes
     * @param visitor The implementation of the visitor
     * @param <T> Return type of operation result
     * @return The actual result of operation
     */
    public abstract <T> T accept(ActionVisitor<T> visitor);
}
