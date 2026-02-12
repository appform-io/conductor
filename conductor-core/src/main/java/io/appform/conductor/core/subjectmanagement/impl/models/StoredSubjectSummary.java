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

package io.appform.conductor.server.subjectmanagement.impl.models;

import io.appform.conductor.model.subject.Gender;
import io.appform.conductor.server.utils.Constants;
import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.val;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Storage representation for {@link io.appform.conductor.model.subject.SubjectSummary}
 */
@Entity
@Table(name = StoredSubjectSummary.SUBJECT_TABLE_NAME)
@Getter
@Setter
@FieldNameConstants
@ToString
@SQLDelete(sql = "update subject_summaries set deleted=true where global_id=?")
public class StoredSubjectSummary {
    public static final String SUBJECT_TABLE_NAME = "subject_summaries";

    @Id
    @LookupKey
    @Column(name = "global_id", nullable = false, unique = true, length = Constants.MAX_SUBJECT_GLOBAL_ID_LENGTH)
    private String globalId;

    @Column(name = "name", length = 45)
    private String name;

    @Column(name = "dob", columnDefinition = "timestamp")
    private Date dob;

    @Column(name = "gender", length = 45)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
    private Date updated;

    @Transient
    private List<StoredSubjectID> ids;

    @Transient
    private List<StoredAddress> addresses;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        val that = (StoredSubjectSummary) o;
        return Objects.equals(getGlobalId(), that.getGlobalId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
