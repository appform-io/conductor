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

package io.appform.conductor.model.usermgmt;

import lombok.Value;
import lombok.With;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * A user for the system. This includes operators, administrators etc
 */
@Value
public class UserSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = -8955544854005815841L;

    String id;
    UserType type;
    @With
    String name;
    String email;
    UserState state;
    Date created;
    Date updated;
}
