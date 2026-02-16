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

package io.appform.conductor.console.ui.views.admin;

import io.appform.conductor.console.ui.views.BaseLoggedInView;
import io.appform.conductor.model.auth.Role;
import io.appform.conductor.model.events.analytics.ObjectReference;
import io.appform.conductor.model.events.impl.ReferredObjectType;
import io.appform.conductor.model.skills.SkillValue;
import io.appform.conductor.model.usermgmt.Group;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.model.usermgmt.UserSessionDetails;
import io.appform.conductor.core.attributes.values.AttributeManager.MaterializedAttributeValue;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserAdminView extends BaseLoggedInView {
    User userDetails;
    List<Role> availableRoles;
    List<Group> availableGroups;
    List<SkillValue> availableSkills;
    List<MaterializedAttributeValue> attributes;
    String attributeUrl;
    List<UserSessionDetails> sessions;

    public UserAdminView(
            User currentUser,
            User userDetails,
            List<Role> availableRoles,
            List<Group> availableGroups,
            List<SkillValue> availableSkills,
            List<MaterializedAttributeValue> attributes,
            List<UserSessionDetails> sessions) {
        super("templates/admin/user-admin.hbs",
              currentUser,
              null != userDetails
              ? new ObjectReference(ReferredObjectType.USER, userDetails.getSummary().getId())
              : null);
        this.userDetails = userDetails;
        this.availableRoles = availableRoles;
        this.availableGroups = availableGroups;
        this.availableSkills = availableSkills;
        this.attributes = attributes;
        this.attributeUrl = null != userDetails
                       ? "/admin/users/" + userDetails.getSummary().getId() + "/attributes"
                       : "";
        this.sessions = sessions;
    }
}
