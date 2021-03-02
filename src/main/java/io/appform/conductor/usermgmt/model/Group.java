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

package io.appform.conductor.usermgmt.model;

import lombok.Value;

import java.util.Date;

/**
 * A group of users in the system. Ticket routing will be done to groups or users.
 */
@Value
public class Group {
    String id;
    String name;
    String description;
    boolean deleted;
    Date created;
    Date updated;
}
