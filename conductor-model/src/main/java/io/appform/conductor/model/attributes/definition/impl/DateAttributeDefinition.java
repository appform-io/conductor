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

package io.appform.conductor.model.attributes.definition.impl;

import io.appform.conductor.model.attributes.AttributeType;
import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.attributes.definition.AttributeDefinitionVisitor;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.Serial;
import java.util.Date;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DateAttributeDefinition extends AttributeDefinition {

    @Serial
    private static final long serialVersionUID = 1793205484491368828L;

    @Jacksonized
    @Builder
    public DateAttributeDefinition(
            String id,
            String name,
            String displayName,
            String description,
            Date created,
            Date updated) {
        super(AttributeType.DATE, id, name, displayName, description, created, updated);
    }

    @Override
    public <T> T accept(AttributeDefinitionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public DateAttributeDefinition withDisplayName(String displayName) {
        return new DateAttributeDefinition(getId(),
                                           getName(),
                                           displayName,
                                           getDescription(),
                                           getCreated(),
                                           getUpdated());
    }

    @Override
    public AttributeDefinition withDescription(String description) {
        return new DateAttributeDefinition(getId(),
                                           getName(),
                                           getDisplayName(),
                                           description,
                                           getCreated(),
                                           getUpdated());
    }
}
