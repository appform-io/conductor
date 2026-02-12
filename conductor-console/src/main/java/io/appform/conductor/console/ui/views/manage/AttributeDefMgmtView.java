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

package io.appform.conductor.server.ui.views.manage;

import io.appform.conductor.model.attributes.AttributeScopeType;
import io.appform.conductor.model.attributes.AttributeType;
import io.appform.conductor.model.attributes.definition.AttributeDefinition;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.EnumSet;
import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AttributeDefMgmtView extends BaseLoggedInView {
    AttributeScopeType scopeType;
    List<AttributeDefinition> attrDefs;
    AttributeDefinition current;
    EnumSet<AttributeType> attributeTypes = EnumSet.allOf(AttributeType.class);
    public AttributeDefMgmtView(
            User currentUser, AttributeScopeType scopeType,
            List<AttributeDefinition> attrDefs,
            AttributeDefinition current) {
        super("templates/manage/attribute-definitons.hbs", currentUser);
        this.scopeType = scopeType;
        this.attrDefs = attrDefs;
        this.current = current;
    }
}
