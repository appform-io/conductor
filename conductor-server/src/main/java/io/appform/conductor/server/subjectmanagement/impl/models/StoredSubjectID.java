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

import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.subject.SubjectIDVerificationStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SQLDelete;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * DB representation for {@link io.appform.conductor.model.subject.SubjectID}
 */
@Entity
@Table(name = StoredSubjectID.SUBJECT_ID_TABLE_NAME,
        indexes = {
                @Index(name = "idx_sub_id", columnList = "id_type, id_value"),
                @Index(name = "idx_sub_subtype_id", columnList = "id_type, sub_type, id_value"),
                @Index(name = "idx_sub_global", columnList = "subject_global_id")
        })
@Getter
@Setter
@ToString
@SQLDelete(sql = "update subject_ids set deleted=true where external_id=?")
public class StoredSubjectID {
    public static final String SUBJECT_ID_TABLE_NAME = "subject_ids";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "external_id", unique = true)
    private String extId;

    @Column(name = "id_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubjectIDType type;

    @Column(name = "sub_type")
    private String subType;

    @Column(name = "id_value", nullable = false)
    private String value;

    @Column(name = "subject_global_id", nullable = false)
    private String subjectGlobalId;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "verification_status")
    @Enumerated(EnumType.STRING)
    private SubjectIDVerificationStatus verificationStatus;

    @Column
    private boolean deleted;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp",
            updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        val that = (StoredSubjectID) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
