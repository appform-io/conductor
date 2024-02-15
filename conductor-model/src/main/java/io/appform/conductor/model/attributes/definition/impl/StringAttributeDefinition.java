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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.Serial;
import java.util.Date;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StringAttributeDefinition extends AttributeDefinition {

    @Serial
    private static final long serialVersionUID = -7386079080770866240L;

    int maxLength;

    String pattern;

    @Jacksonized
    @Builder
    public StringAttributeDefinition(
            String id,
            String name,
            String displayName,
            String description,
            Date created,
            Date updated,
            int maxLength,
            String pattern) {
        super(AttributeType.STRING, id, name, displayName, description, created, updated);
        this.maxLength = maxLength;
        this.pattern = pattern;
    }

    @Override
    public <T> T accept(AttributeDefinitionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public StringAttributeDefinition withDisplayName(String displayName) {
        return new StringAttributeDefinition(getId(),
                                             getName(),
                                             displayName,
                                             getDescription(),
                                             getCreated(),
                                             getUpdated(),
                                             getMaxLength(),
                                             getPattern());
    }

    @Override
    public AttributeDefinition withDescription(String description) {
        return new StringAttributeDefinition(getId(),
                                             getName(),
                                             getDisplayName(),
                                             description,
                                             getCreated(),
                                             getUpdated(),
                                             getMaxLength(),
                                             getPattern());
    }

    public StringAttributeDefinition withMaxLength(final int maxLength) {
        return new StringAttributeDefinition(getId(),
                                             getName(),
                                             getDisplayName(),
                                             getDescription(),
                                             getCreated(),
                                             getUpdated(),
                                             maxLength,
                                             getPattern());
    }

    public StringAttributeDefinition withPattern(final String pattern) {
        return new StringAttributeDefinition(getId(),
                                             getName(),
                                             getDisplayName(),
                                             getDescription(),
                                             getCreated(),
                                             getUpdated(),
                                             getMaxLength(),
                                             pattern);
    }
}
