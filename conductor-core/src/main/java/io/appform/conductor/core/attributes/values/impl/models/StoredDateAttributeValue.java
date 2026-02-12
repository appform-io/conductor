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

package io.appform.conductor.server.attributes.values.impl.models;

import io.appform.conductor.model.attributes.AttributeType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serial;
import java.util.Date;

/**
 *
 */
@Entity
@Getter
@Setter
@DiscriminatorValue(AttributeType.Values.DATE_TYPE)
@ToString(callSuper = true)
public class StoredDateAttributeValue extends StoredAttributeValue {
    @Serial
    private static final long serialVersionUID = -8604433704525154605L;

    @Column(name = "date_value")
    private Date dateValue;

    public StoredDateAttributeValue() {
        super(AttributeType.DATE);
    }

    @Override
    public <T> T accept(StoredAttributeValueVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
