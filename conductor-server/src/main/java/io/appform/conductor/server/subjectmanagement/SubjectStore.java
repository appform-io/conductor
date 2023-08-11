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

package io.appform.conductor.server.subjectmanagement;

import io.appform.conductor.model.subject.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * A store for subjects
 */
public interface SubjectStore {

    Optional<SubjectSummary> saveSubject(List<SubjectID> ids, String globalSubjectId, String name, Date dob);

    Optional<Subject> updateSubject(String globalSubjectId, UnaryOperator<Subject> updater);
    Optional<Subject> updateSubjectSummary(String globalSubjectId, UnaryOperator<SubjectSummary> updater);

    Optional<Subject> getSubject(String globalSubjectId);

    Optional<SubjectSummary> getSubjectSummary(final String globalSubjectId);

    boolean deleteSubject(final String globalSubjectId);

    Optional<SubjectID> saveIdentifier(
            String globalSubjectId,
            final SubjectIDType type,
            String subType,
            String value,
            SubjectIDVerificationStatus status);

    Optional<SubjectID> updateIdentifier(final String globalSubjectId, String idExtId, UnaryOperator<SubjectID> updater);
    Optional<SubjectID> markIdentifierAsPrimary(final String globalSubjectId, String idExtId);
    boolean deleteIdentifier(final String globalSubjectId, String idExtId);

    List<SubjectSummary> lookupSummaryById(final SubjectID id);

    Optional<Address> saveAddress(
            final String globalSubjectId,
            final AddressType type,
            final String houseNumber,
            final String street,
            final String locality,
            final String city,
            final String state,
            final String country,
            final String pinCode);

    Optional<Address> updateAddress(final String globalSubjectId,
                                    final String id,
                                    final UnaryOperator<Address> address);

    boolean deleteAddress(final String globalSubjectId, String id);

    List<Address> findAddressesForSubject(final String globalSubjectId);
}
