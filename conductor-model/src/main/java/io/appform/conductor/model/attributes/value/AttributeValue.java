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

package io.appform.conductor.model.attributes.value;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.conductor.model.attributes.AttributeType;
import io.appform.conductor.model.attributes.value.impl.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 *
 */
@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "STRING", value = StringAttributeValue.class),
        @JsonSubTypes.Type(name = "NUMBER", value = NumberAttributeValue.class),
        @JsonSubTypes.Type(name = "CHOICE", value = ChoiceAttributeValue.class),
        @JsonSubTypes.Type(name = "DATE", value = DateAttributeValue.class),
        @JsonSubTypes.Type(name = "LINK", value = LinkAttributeValue.class),
})
public abstract class AttributeValue {
    private final AttributeType type;
    private final String schemaId;
    private final Date created;
    private final Date updated;

    public abstract <T> T accept(final AttributeValueVisitor<T> visitor);
}
