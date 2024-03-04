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

package io.appform.conductor.model.attributes.value.impl;

import io.appform.conductor.model.attributes.AttributeType;
import io.appform.conductor.model.attributes.value.AttributeValue;
import io.appform.conductor.model.attributes.value.AttributeValueVisitor;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ChoiceAttributeValue extends AttributeValue {
    List<String> value;

    public ChoiceAttributeValue(
            String schemaId,
            List<String> value) {
        this(schemaId, null, null, value);
    }

    @Builder
    @Jacksonized
    public ChoiceAttributeValue(
            String schemaId,
            Date created,
            Date updated,
            List<String> value) {
        super(AttributeType.CHOICE, schemaId, created, updated);
        this.value = value;
    }

    public ChoiceAttributeValue withValue(final String... value) {
        return new ChoiceAttributeValue(this.getSchemaId(),
                                        this.getCreated(),
                                        this.getUpdated(),
                                        Arrays.asList(value));
    }

    public ChoiceAttributeValue withValue(final List<String> value) {
        return new ChoiceAttributeValue(this.getSchemaId(),
                                        this.getCreated(),
                                        this.getUpdated(),
                                        value);
    }


    @Override
    public <T> T accept(AttributeValueVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
