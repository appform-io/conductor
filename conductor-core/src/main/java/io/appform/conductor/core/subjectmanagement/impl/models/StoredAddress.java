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

package io.appform.conductor.core.subjectmanagement.impl.models;

import io.appform.conductor.model.subject.AddressType;
import io.appform.conductor.core.utils.Constants;
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
 * DB representation for an address
 */
@Entity
@Table(name = StoredAddress.SUBJECT_ADDRESS_TABLE_NAME, indexes = {
        @Index(name = "idx_subject_global_id", columnList = "subject_global_id")
})
@Getter
@Setter
@FieldNameConstants
@ToString
@SQLDelete(sql = "update addresses set deleted=true where address_id=?")
public class StoredAddress {
    public static final String SUBJECT_ADDRESS_TABLE_NAME = "addresses";

    @Id
    @Column(name = "address_id", nullable = false, unique = true, length = Constants.MAX_ADDRESS_ID_LENGTH)
    private String addressId;

    @Column(name = "type", length = 45)
    @Enumerated(EnumType.STRING)
    private AddressType type;

    @Column(name = "house_number", length = 255)
    private String houseNumber;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "locality", length = 255)
    private String locality;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "state", length = 255)
    private String state;

    @Column(name = "country", length = 255)
    private String country;

    @Column(name = "pin_code", length = 255)
    private String pinCode;

    @Column(name = "subject_global_id", nullable = false, length = Constants.MAX_SUBJECT_GLOBAL_ID_LENGTH)
    String subjectGlobalId;

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
        val that = (StoredAddress) o;
        return Objects.equals(getAddressId(), that.getAddressId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
