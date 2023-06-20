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

import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.subject.*;
import io.appform.conductor.server.subjectmanagement.SubjectStore;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredAddress;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectID;
import io.appform.conductor.server.subjectmanagement.impl.models.StoredSubjectSummary;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Implementation for {@link SubjectStore} that stores data on RDBMS
 */
@Singleton
@Slf4j
public class DBSubjectStore implements SubjectStore {

    private final LookupDao<StoredSubjectSummary> subjectDao;
    private final RelationalDao<StoredSubjectID> subjectIdDao;
    private final RelationalDao<StoredAddress> addressDao;

    @Inject
    public DBSubjectStore(
            LookupDao<StoredSubjectSummary> subjectDao,
            RelationalDao<StoredSubjectID> subjectIdDao,
            RelationalDao<StoredAddress> addressDao) {
        this.subjectDao = subjectDao;
        this.subjectIdDao = subjectIdDao;
        this.addressDao = addressDao;
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectSummary.SUBJECT_TABLE_NAME))
    public Optional<Subject> saveSubject(
            List<SubjectID> ids,
            @Throws.RuntimeParam("id") String globalSubjectId,
            String name,
            Date dob) {
        val storedSummary = new StoredSubjectSummary()
                .setGlobalId(globalSubjectId)
                .setName(name)
                .setDob(dob);
        val savedSummary = subjectDao.saveAndGetExecutor(storedSummary)
                .saveAll(subjectIdDao,
                         summary -> ids.stream()
                                 .map(subId -> new StoredSubjectID()
                                         .setType(subId.getType())
                                         .setSubType(subId.getSubType())
                                         .setExtId(UUID.randomUUID().toString())
                                         .setValue(subId.getValue())
                                         .setSubjectGlobalId(summary.getGlobalId())
                                         .setPrimary(subId.isPrimary())
                                         .setVerificationStatus(SubjectIDVerificationStatus.UNVERIFIED)
                                     )
                                 .toList())
                .execute();
        return Optional.ofNullable(savedSummary)
                .flatMap(summary -> getSubject(summary.getGlobalId()));
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectSummary.SUBJECT_TABLE_NAME))
    public Optional<Subject> updateSubject(
            @Throws.RuntimeParam("id") String globalSubjectId,
            UnaryOperator<Subject> updater) {
        val updatedRes = subjectDao.update(globalSubjectId,
                                           subOptional -> subOptional.map(stored -> {
                                                       val updated = updater.apply(toWire(stored));
                                                       if (null == updated) {
                                                           return null;
                                                       }
                                                       return stored
                                                               .setName(updated.getSummary().getName())
                                                               .setDob(updated.getSummary().getDob());
                                                   })
                                                   .orElse(null));
        if (updatedRes) {
            return getSubject(globalSubjectId);
        }
        return Optional.empty();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectSummary.SUBJECT_TABLE_NAME))
    public Optional<Subject> getSubject(@Throws.RuntimeParam("id") String subjectId) {
        return subjectDao.readOnlyExecutor(subjectId)
                .readAugmentParent(subjectIdDao,
                                   criteriaForSubject(subjectId, StoredSubjectID.class),
                                   1, Integer.MAX_VALUE,
                                   StoredSubjectSummary::setIds)
                .readAugmentParent(addressDao,
                                   criteriaForSubject(subjectId, StoredAddress.class),
                                   1, Integer.MAX_VALUE,
                                   StoredSubjectSummary::setAddresses)
                .execute()
                .map(DBSubjectStore::toWire);
    }

    @Override
    @SneakyThrows
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectSummary.SUBJECT_TABLE_NAME))
    public Optional<SubjectSummary> getSubjectSummary(@Throws.RuntimeParam("id") String globalSubjectId) {
        return Optional.of(subjectDao.get(globalSubjectId, DBSubjectStore::toWireSummary));
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectSummary.SUBJECT_TABLE_NAME))
    public boolean deleteSubject(@Throws.RuntimeParam("id") String globalSubjectId) {
        val updated = subjectDao.lockAndGetExecutor(globalSubjectId)
                .mutate(summary -> summary.setDeleted(true))
                .saveAll(subjectIdDao, summary -> Objects.requireNonNullElse(summary.getIds(),
                                                                             Collections.<StoredSubjectID>emptyList())
                        .stream()
                        .map(id -> id.setDeleted(true))
                        .toList())
                .saveAll(addressDao, summary -> Objects.requireNonNullElse(summary.getAddresses(),
                                                                           Collections.<StoredAddress>emptyList())
                        .stream()
                        .map(id -> id.setDeleted(true))
                        .toList())
                .execute();
        return updated.isDeleted();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectID.SUBJECT_ID_TABLE_NAME))
    public Optional<SubjectID> saveIdentifier(
            @Throws.RuntimeParam("id") String globalSubjectId,
            SubjectIDType type,
            String subType,
            String value,
            SubjectIDVerificationStatus status) {
        val extId = generateIdForSubjectID(globalSubjectId, type, subType, value);
        val updatedSummary = subjectDao.lockAndGetExecutor(globalSubjectId) //For safety
                .save(subjectIdDao, summary -> new StoredSubjectID()
                        .setSubjectGlobalId(summary.getGlobalId())
                        .setType(type)
                        .setSubType(subType)
                        .setExtId(extId)
                        .setValue(value)
                        .setPrimary(false)
                        .setVerificationStatus(status))
                .execute();
        if (null == updatedSummary) {
            return Optional.empty();
        }
        return readSubjectId(globalSubjectId, extId);
    }


    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectID.SUBJECT_ID_TABLE_NAME))
    public Optional<SubjectID> updateIdentifier(
            String globalSubjectId,
            @Throws.RuntimeParam("id") String idExtId,
            UnaryOperator<SubjectID> updater) {
        val status = subjectDao.lockAndGetExecutor(globalSubjectId) //For safety
                .update(subjectIdDao,
                        DetachedCriteria.forClass(StoredSubjectID.class)
                                .add(Property.forName("extId").eq(idExtId))
                                .add(Property.forName("subjectGlobalId").eq(globalSubjectId)),
                        subjectId -> {
                            val updated = updater.apply(toWire(subjectId));
                            return subjectId.setValue(updated.getValue())
                                    .setPrimary(updated.isPrimary())
                                    .setVerificationStatus(updated.getVerificationStatus())
                                    .setDeleted(updated.isDeleted());
                        },
                        () -> false)
                .execute() != null;
        log.info("Update statud for id {}/{}: {}", globalSubjectId, idExtId, status);
        return readSubjectId(globalSubjectId, idExtId);
    }

    @Override
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectSummary.SUBJECT_TABLE_NAME))
    public List<SubjectSummary> lookupSummaryById(SubjectID id) {
        val subGlobalIds = fetchSubjectIdsFoId(id);
        return subjectDao.scatterGather(DetachedCriteria.forClass(StoredSubjectSummary.class)
                                                .add(Property.forName("globalId").in(subGlobalIds)))
                .stream()
                .map(DBSubjectStore::toWireSummary)
                .toList();
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAddress.SUBJECT_ADDRESS_TABLE_NAME))
    public Optional<Address> saveAddress(
            @Throws.RuntimeParam("id") String globalSubjectId,
            AddressType type,
            String houseNumber,
            String street,
            String locality,
            String city,
            String state,
            String country,
            String pinCode) {
        return addressDao.save(globalSubjectId,
                               new StoredAddress()
                                       .setType(type)
                                       .setHouseNumber(houseNumber)
                                       .setStreet(street)
                                       .setLocality(locality)
                                       .setCity(city)
                                       .setState(state)
                                       .setCountry(country)
                                       .setPinCode(pinCode)
                                       .setSubjectGlobalId(globalSubjectId)
                                       .setDeleted(false))
                .map(DBSubjectStore::toWire);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAddress.SUBJECT_ADDRESS_TABLE_NAME))
    public Optional<Address> updateAddress(
            String globalSubjectId,
            @Throws.RuntimeParam("id") String extId,
            UnaryOperator<Address> updater) {
        val criteria = DetachedCriteria.forClass(StoredAddress.class)
                .add(Property.forName("subjectGlobalId").eq(globalSubjectId))
                .add(Property.forName("extId").eq(extId));
        addressDao.update(globalSubjectId,
                          criteria,
                          stored -> {
                              val updated = updater.apply(toWire(stored));
                              return stored
                                      .setType(updated.getType())
                                      .setHouseNumber(updated.getHouseNumber())
                                      .setStreet(updated.getStreet())
                                      .setLocality(updated.getLocality())
                                      .setCity(updated.getCity())
                                      .setState(updated.getState())
                                      .setCountry(updated.getCountry())
                                      .setPinCode(updated.getPinCode());
                          });
        return addressDao.select(globalSubjectId, criteria, 0, 1)
                .stream()
                .map(DBSubjectStore::toWire).findAny();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAddress.SUBJECT_ADDRESS_TABLE_NAME))
    public boolean deleteAddress(String globalSubjectId, String extId) {
        val criteria = DetachedCriteria.forClass(StoredAddress.class)
                .add(Property.forName("subjectGlobalId").eq(globalSubjectId))
                .add(Property.forName("extId").eq(extId));
        return addressDao.update(globalSubjectId,
                                 criteria,
                                 stored -> stored.setDeleted(true));
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredAddress.SUBJECT_ADDRESS_TABLE_NAME))
    public List<Address> findAddressesForSubject(String globalSubjectId) {
        return addressDao.select(globalSubjectId,
                                 DetachedCriteria.forClass(StoredAddress.class)
                                         .add(Property.forName("subjectGlobalId").eq(globalSubjectId))
                                         .add(Property.forName("deleted").eq(false)),
                                 0,
                                 Integer.MAX_VALUE)
                .stream()
                .map(DBSubjectStore::toWire)
                .toList();
    }

    private static <T> DetachedCriteria criteriaForSubject(String id, Class<T> clazz) {
        return DetachedCriteria.forClass(clazz)
                .add(Property.forName("subjectGlobalId").eq(id))
                .add(Property.forName("deleted").eq(false));
    }

    private static Subject toWire(final StoredSubjectSummary stored) {
        return new Subject(toWireSummary(stored),
                           Objects.requireNonNullElse(stored.getIds(), new ArrayList<StoredSubjectID>())
                                   .stream()
                                   .map(DBSubjectStore::toWire)
                                   .toList(),
                           Objects.requireNonNullElse(stored.getAddresses(), Collections.<StoredAddress>emptyList())
                                   .stream()
                                   .map(DBSubjectStore::toWire)
                                   .toList(),
                           false);
    }

    private static SubjectSummary toWireSummary(final StoredSubjectSummary stored) {
        return new SubjectSummary(stored.getGlobalId(),
                                  stored.getName(),
                                  stored.getDob(),
                                  stored.isDeleted(),
                                  stored.getCreated(),
                                  stored.getUpdated());
    }

    private static SubjectID toWire(final StoredSubjectID subjectID) {
        return new SubjectID(subjectID.getType(),
                             subjectID.getSubType(),
                             subjectID.getExtId(),
                             subjectID.getValue(),
                             subjectID.isPrimary(),
                             subjectID.getVerificationStatus(),
                             subjectID.isDeleted(),
                             subjectID.getCreated(),
                             subjectID.getUpdated());
    }

    private static Address toWire(final StoredAddress address) {
        return new Address(address.getExtId(),
                           address.getType(),
                           address.getHouseNumber(),
                           address.getStreet(),
                           address.getLocality(),
                           address.getCity(),
                           address.getState(),
                           address.getCountry(),
                           address.getPinCode());
    }

    private static String generateIdForSubjectID(
            String globalSubjectId,
            SubjectIDType type,
            String subType,
            String value) {
        return UUID.nameUUIDFromBytes(String.format("%s-%s-%s-%s", globalSubjectId, type, subType, value).getBytes())
                .toString();
    }

    @SneakyThrows
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredSubjectID.SUBJECT_ID_TABLE_NAME))
    private Optional<SubjectID> readSubjectId(String globalSubjectId, @Throws.RuntimeParam("id") String extId) {
        return subjectIdDao.select(globalSubjectId,
                                   criteriaForSubject(globalSubjectId, StoredSubjectID.class)
                                           .add(Property.forName("extId").eq(extId)),
                                   0, 1)
                .stream()
                .findAny()
                .map(DBSubjectStore::toWire);
    }

    private List<String> fetchSubjectIdsFoId(SubjectID id) {
        return subjectIdDao.scatterGather(DetachedCriteria.forClass(StoredSubjectID.class)
                                                  .add(Property.forName("type").eq(id.getType()))
                                                  .add(Property.forName("subType").eq(id.getSubType()))
                                                  .add(Property.forName("value").eq(id.getValue())),
                                          0, Integer.MAX_VALUE)
                .stream()
                .map(StoredSubjectID::getSubjectGlobalId)
                .distinct()
                .toList();
    }

}
