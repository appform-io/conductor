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

package io.appform.conductor.model.usermgmt;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * A group of users in the system. Ticket routing will be done to groups or users.
 */
@Value
@AllArgsConstructor
public class Group implements Serializable {
    @Serial
    private static final long serialVersionUID = -7955764341889247005L;

    String id;
    String name;
    @With
    String description;
    @With
    GroupType type;
    @With
    Set<String> requiredSkills;
    @With
    boolean deleted;
    Date created;
    Date updated;
}
