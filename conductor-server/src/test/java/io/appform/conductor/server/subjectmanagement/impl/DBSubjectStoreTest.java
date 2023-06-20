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

package io.appform.conductor.server.subjectmanagement.impl;

import io.appform.conductor.model.subject.SubjectID;
import io.appform.conductor.model.subject.SubjectIDType;
import io.appform.conductor.model.subject.SubjectIDVerificationStatus;
import io.appform.conductor.server.DBTestBase;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredAddress;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectID;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectSummary;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class DBSubjectStoreTest extends DBTestBase {

    @Test
    void testSummaryCRUD() {
        val ds = new DBSubjectStore(bundle.createParentObjectDao(StoredSubjectSummary.class),
                                    bundle.createRelatedObjectDao(StoredSubjectID.class),
                                    bundle.createRelatedObjectDao(StoredAddress.class));
        val created = ds.saveSubject(List.of(new SubjectID(SubjectIDType.PHONE,
                                             "NA",
                                             null,
                                             "9999999999",
                                             false,
                                             SubjectIDVerificationStatus.UNVERIFIED,
                                             false,
                                             null,
                                             null)),
                       "test",
                       "Sad Robot",
                       new Date())
                .orElse(null);
        assertNotNull(created);
        assertEquals(created.getSummary(), ds.lookupSummaryById(new SubjectID(SubjectIDType.PHONE,
                                                         "NA",
                                                         null,
                                                         "9999999999",
                                                         false,
                                                         SubjectIDVerificationStatus.UNVERIFIED,
                                                         false,
                                                         null,
                                                         null))
                .stream().findFirst().orElse(null));
    }

}