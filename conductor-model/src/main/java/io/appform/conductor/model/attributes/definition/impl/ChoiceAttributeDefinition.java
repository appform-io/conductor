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
import java.util.Set;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ChoiceAttributeDefinition extends AttributeDefinition {

    @Serial
    private static final long serialVersionUID = -5273354349593693029L;

    @Singular
    Set<String> options;

    boolean allowMultiple;

    @Jacksonized
    @Builder
    public ChoiceAttributeDefinition(
            String id,
            String name,
            String displayName,
            String description,
            Date created,
            Date updated,
            Set<String> options, boolean allowMultiple) {
        super(AttributeType.CHOICE, id, name, displayName, description, created, updated);
        this.options = options;
        this.allowMultiple = allowMultiple;
    }

    @Override
    public <T> T accept(AttributeDefinitionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public ChoiceAttributeDefinition withDisplayName(String displayName) {
        return new ChoiceAttributeDefinition(getId(),
                                             getName(),
                                             displayName,
                                             getDescription(),
                                             getCreated(),
                                             getUpdated(),
                                             getOptions(),
                                             isAllowMultiple());
    }

    @Override
    public AttributeDefinition withDescription(String description) {
        return new ChoiceAttributeDefinition(getId(),
                                             getName(),
                                             getDisplayName(),
                                             description,
                                             getCreated(),
                                             getUpdated(),
                                             getOptions(),
                                             isAllowMultiple());
    }

    public AttributeDefinition withOptions(Set<String> options) {
        return new ChoiceAttributeDefinition(getId(),
                                             getName(),
                                             getDisplayName(),
                                             getDescription(),
                                             getCreated(),
                                             getUpdated(),
                                             options,
                                             isAllowMultiple());
    }

    public AttributeDefinition withAllowMultiple(boolean allowMultiple) {
        return new ChoiceAttributeDefinition(getId(),
                                             getName(),
                                             getDisplayName(),
                                             getDescription(),
                                             getCreated(),
                                             getUpdated(),
                                             getOptions(),
                                             allowMultiple);
    }
}
