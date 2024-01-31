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
import io.appform.conductor.server.utils.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.val;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
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
@FieldNameConstants
@SQLDelete(sql = "update subject_ids set deleted=true where external_id=?")
public class StoredSubjectID {
    public static final String SUBJECT_ID_TABLE_NAME = "subject_ids";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "external_id", unique = true, length = 255)
    private String extId;

    @Column(name = "id_type", nullable = false, length = 45)
    @Enumerated(EnumType.STRING)
    private SubjectIDType type;

    @Column(name = "sub_type", length = 45)
    private String subType;

    @Column(name = "id_value", nullable = false, length = 45)
    private String value;

    @Column(name = "subject_global_id", nullable = false, length = Constants.MAX_SUBJECT_GLOBAL_ID_LENGTH)
    private String subjectGlobalId;

    @Column(name = "is_primary")
    private boolean primary;

    @Column(name = "verification_status",length = 45)
    @Enumerated(EnumType.STRING)
    private SubjectIDVerificationStatus verificationStatus;

    @Column(name = "deleted")
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created", columnDefinition = Constants.CREATED_DATE_DEFINITION)
    private Date created;

    @UpdateTimestamp
    @Column(name = "updated", columnDefinition = Constants.UPDATED_DATE_DEFINITION)
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
