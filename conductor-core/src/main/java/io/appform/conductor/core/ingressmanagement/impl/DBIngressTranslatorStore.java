package io.appform.conductor.core.ingressmanagement.impl;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.Throws;
import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.core.actionmanagement.impl.models.StoredAction;
import io.appform.conductor.core.ingressmanagement.IngressTranslatorStore;
import io.appform.conductor.core.ingressmanagement.impl.models.StoredIngressTranslator;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

import static io.appform.conductor.model.error.ConductorErrorCode.STORE_UPDATE_ERROR;
import static io.appform.conductor.model.error.ConductorErrorCode.STORE_WRITE_ERROR;
import static io.appform.conductor.core.utils.ConductorServerUtils.readableId;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DBIngressTranslatorStore implements IngressTranslatorStore {

    private final LookupDao<StoredIngressTranslator> ingressTranslatorLookupDao;

    @Override
    @SneakyThrows
    @Throws(value = ConductorErrorCode.STORE_READ_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredIngressTranslator.INGRESS_TRANSLATOR_TABLE_NAME))
    public Optional<IngressTranslator> read(@Throws.RuntimeParam("id") String id) {
        return ingressTranslatorLookupDao.get(id).map(this::toWired);
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredIngressTranslator.INGRESS_TRANSLATOR_TABLE_NAME))
    public List<IngressTranslator> read(List<String> ids) {
        return ingressTranslatorLookupDao.get(ids)
                .stream()
                .filter(g -> !g.isDeleted())
                .map(this::toWired)
                .toList();
    }

    @Override
    @MonitoredFunction
    @Throws(value = ConductorErrorCode.STORE_LIST_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredIngressTranslator.INGRESS_TRANSLATOR_TABLE_NAME))
    public List<IngressTranslator> list(Scope scope) {
        return ingressTranslatorLookupDao.scatterGather(DetachedCriteria.forClass(StoredIngressTranslator.class)
                                                            .add(Property.forName(StoredAction.Fields.scopeType).eq(scope.getType()))
                                                            .add(Property.forName(StoredAction.Fields.scopeReferenceId).in(scope.getReferenceId())))
                .stream()
                .filter(translator -> !translator.isDeleted())
                .map(this::toWired)
                .toList();
    }


    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredIngressTranslator.INGRESS_TRANSLATOR_TABLE_NAME))
    public Optional<IngressTranslator> createOrUpdate(String name, String description, String ticketIdPath,
                                                      Template template, Scope scope) {
        val id = readableId(name);
        return ingressTranslatorLookupDao.createOrUpdate(
                        id,
                        existing -> existing.setTemplate(template)
                                .setTicketIdPath(ticketIdPath)
                                .setDescription(description)
                                .setDeleted(false),
                        () -> toStored(new IngressTranslator(id, name, description, ticketIdPath,
                                template, scope.getType(), scope.getReferenceId(), null, null)))
                .map(this::toWired);
    }

    @Override
    @MonitoredFunction
    @SneakyThrows
    @Throws(value = STORE_UPDATE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredIngressTranslator.INGRESS_TRANSLATOR_TABLE_NAME))
    public Optional<IngressTranslator> update(@Throws.RuntimeParam("id") String id,
                                              String description,
                                              String ticketIdPath,
                                              Template template) {
        ingressTranslatorLookupDao.update(id,
                storedIngressTranslator -> storedIngressTranslator.map(s -> s.setTemplate(template)
                                .setTicketIdPath(ticketIdPath)
                                .setDescription(description))
                        .orElse(null));
        return read(id);
    }



    @Override
    @Throws(value = ConductorErrorCode.STORE_WRITE_ERROR,
            fixedParams = @Throws.Param(name = "type", value = StoredIngressTranslator.INGRESS_TRANSLATOR_TABLE_NAME))
    public boolean delete(@Throws.RuntimeParam("id") String id) {
        return ingressTranslatorLookupDao.update(id,
                existing -> existing.map(stored -> stored.setDeleted(true)).orElse(null));
    }


    private StoredIngressTranslator toStored(IngressTranslator translator) {
        return new StoredIngressTranslator()
                .setId(translator.getId())
                .setName(translator.getName())
                .setDescription(translator.getDescription())
                .setTicketIdPath(translator.getTicketIdPath())
                .setTemplate(translator.getTemplate())
                .setScopeReferenceId(translator.getScopeReferenceId())
                .setScopeType(translator.getScopeType());
    }

    private IngressTranslator toWired(StoredIngressTranslator translator) {
        return new IngressTranslator(translator.getId(),
                translator.getName(),
                translator.getDescription(),
                translator.getTicketIdPath(),
                translator.getTemplate(),
                translator.getScopeType(),
                translator.getScopeReferenceId(),
                translator.getCreated(),
                translator.getUpdated());
    }


}
