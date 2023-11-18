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

package io.appform.conductor.server.ui.views.subjects;

import io.appform.conductor.model.subject.Gender;
import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.subject.SubjectSummary;
import io.appform.conductor.model.usermgmt.User;
import io.appform.conductor.server.eventmanagement.events.ReferredObjectType;
import io.appform.conductor.server.eventmanagement.query.ObjectReference;
import io.appform.conductor.server.ui.views.BaseLoggedInView;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SubjectListView extends BaseLoggedInView {
    List<SubjectSummary> subjects;
    Set<SubjectIDType> subIdTypes = EnumSet.allOf(SubjectIDType.class);
    Set<Gender> gender = EnumSet.allOf(Gender.class);

    public SubjectListView(User currentUser, List<SubjectSummary> subjects) {
        super("templates/subjects/subject-list.hbs", currentUser, new ObjectReference(ReferredObjectType.SUBJECT, null));
        this.subjects = subjects;
    }
}
