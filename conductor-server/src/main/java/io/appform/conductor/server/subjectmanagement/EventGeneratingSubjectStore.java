package io.appform.conductor.server.subjectmanagement;

import io.appform.conductor.model.subject.*;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import io.appform.conductor.server.eventmanagement.events.*;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Singleton
public class EventGeneratingSubjectStore implements SubjectStore {
    private final EventBus eventBus;
    private final SubjectStore subjectStore;

    @Inject
    public EventGeneratingSubjectStore(EventBus eventBus, @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) SubjectStore subjectStore) {
        this.eventBus = eventBus;
        this.subjectStore = subjectStore;
    }

    @Override
    public Optional<SubjectSummary> saveSubject(List<SubjectID> ids,
                                                String globalSubjectId,
                                                String name,
                                                Date dob,
                                                Gender gender) {
        val res = subjectStore.saveSubject(ids, globalSubjectId, name, dob, gender);
        res.ifPresent(subjectSummary -> eventBus.publish(new SubjectCreatedEvent(subjectSummary.getGlobalId())));
        return res;
    }

    @Override
    public Optional<Subject> updateSubject(String globalSubjectId, UnaryOperator<Subject> updater) {
        val res = subjectStore.updateSubject(globalSubjectId, updater);
        res.ifPresent(subject -> eventBus.publish(new SubjectUpdatedEvent(subject.getSummary().getGlobalId())));
        return res;
    }

    @Override
    public Optional<Subject> updateSubjectSummary(String globalSubjectId, UnaryOperator<SubjectSummary> updater) {
        val res = subjectStore.updateSubjectSummary(globalSubjectId, updater);
        res.ifPresent(subject -> eventBus.publish(new SubjectUpdatedEvent(subject.getSummary().getGlobalId())));
        return res;
    }

    @Override
    public Optional<Subject> getSubject(String globalSubjectId) {
        return subjectStore.getSubject(globalSubjectId);
    }

    @Override
    public Optional<SubjectSummary> getSubjectSummary(String globalSubjectId) {
        return subjectStore.getSubjectSummary(globalSubjectId);
    }

    @Override
    public boolean deleteSubject(String globalSubjectId) {
        val res = subjectStore.deleteSubject(globalSubjectId);
        if(res) {
            eventBus.publish(new SubjectDeletedEvent(globalSubjectId));
        }
        return res;
    }

    @Override
    public Optional<SubjectID> saveIdentifier(String globalSubjectId,
                                              SubjectIDType type,
                                              String subType,
                                              String value,
                                              SubjectIDVerificationStatus status) {
        val res = subjectStore.saveIdentifier(globalSubjectId, type, subType, value, status);
        res.ifPresent(subjectID -> eventBus.publish(new SubjectIdentifierCreatedEvent(globalSubjectId,
                subjectID.getExtId())));
        return res;
    }

    @Override
    public Optional<SubjectID> updateIdentifier(String globalSubjectId,
                                                String idExtId,
                                                UnaryOperator<SubjectID> updater) {
        val res = subjectStore.updateIdentifier(globalSubjectId, idExtId, updater);
        res.ifPresent(subjectID -> eventBus.publish(new SubjectIdentifierUpdatedEvent(globalSubjectId,
                subjectID.getExtId())));
        return res;
    }

    @Override
    public Optional<SubjectID> markIdentifierAsPrimary(String globalSubjectId, String idExtId) {
        val res = subjectStore.markIdentifierAsPrimary(globalSubjectId, idExtId);
        res.ifPresent(subjectID -> eventBus.publish(new SubjectIdentifierMarkedPrimaryEvent(globalSubjectId,
                subjectID.getExtId())));
        return res;
    }

    @Override
    public boolean deleteIdentifier(String globalSubjectId, String idExtId) {
        val res = subjectStore.deleteIdentifier(globalSubjectId, idExtId);
        if(res) {
            eventBus.publish(new SubjectIdentifierDeletedEvent(globalSubjectId, idExtId));
        }
        return res;
    }

    @Override
    public List<SubjectSummary> lookupSummaryById(SubjectID id) {
        return subjectStore.lookupSummaryById(id);
    }

    @Override
    public Optional<Address> saveAddress(String globalSubjectId,
                                         AddressType type,
                                         String houseNumber,
                                         String street,
                                         String locality,
                                         String city,
                                         String state,
                                         String country,
                                         String pinCode) {
        val res = subjectStore.saveAddress(globalSubjectId, type, houseNumber, street, locality,
                city, state, country, pinCode);
        res.ifPresent(address -> eventBus.publish(new AddressCreatedEvent(globalSubjectId, address.getId())));
        return res;
    }

    @Override
    public Optional<Address> updateAddress(String globalSubjectId,
                                           String id,
                                           UnaryOperator<Address> handler) {
        val res = subjectStore.updateAddress(globalSubjectId, id, handler);
        res.ifPresent(address -> eventBus.publish(new AddressUpdatedEvent(globalSubjectId, address.getId())));
        return res;
    }

    @Override
    public boolean deleteAddress(String globalSubjectId, String id) {
        val res = subjectStore.deleteAddress(globalSubjectId, id);
        if(res) {
            eventBus.publish(new AddressDeletedEvent(globalSubjectId, id));
        }
        return res;
    }

    @Override
    public List<Address> findAddressesForSubject(String globalSubjectId) {
        return subjectStore.findAddressesForSubject(globalSubjectId);
    }
}
