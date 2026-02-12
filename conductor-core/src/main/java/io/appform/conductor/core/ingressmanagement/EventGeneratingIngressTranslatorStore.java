package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.events.impl.ingress.IngressTranslatorCreatedEvent;
import io.appform.conductor.model.events.impl.ingress.IngressTranslatorDeletedEvent;
import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.model.workflow.Template;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.eventmanagement.EventBus;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class EventGeneratingIngressTranslatorStore implements IngressTranslatorStore {

    private final EventBus eventBus;
    private final IngressTranslatorStore ingressTranslatorStore;


    @Inject
    public EventGeneratingIngressTranslatorStore(EventBus eventBus,
                                                 @Named(ConductorModule.CACHED_IMPLEMENTATION_NAME) IngressTranslatorStore ingressTranslatorStore) {
        this.eventBus = eventBus;
        this.ingressTranslatorStore = ingressTranslatorStore;
    }


    @Override
    public Optional<IngressTranslator> read(String id) {
        return ingressTranslatorStore.read(id);
    }

    @Override
    public List<IngressTranslator> read(List<String> ids) {
        return ingressTranslatorStore.read(ids);
    }

    @Override
    public List<IngressTranslator> list(Scope scope) {
        return ingressTranslatorStore.list(scope);
    }

    @Override
    public Optional<IngressTranslator> createOrUpdate(String name, String description, String ticketIdPath, Template template, Scope scope) {
        val res =  ingressTranslatorStore.createOrUpdate(name, description, ticketIdPath, template, scope);
        res.ifPresent(newTask -> eventBus.publish(new IngressTranslatorCreatedEvent(newTask.getId())));
        return res;
    }


    @Override
    public Optional<IngressTranslator> update(String id, String description, String ticketIdPath, Template template) {
        val res =  ingressTranslatorStore.update(id, description, ticketIdPath, template);
        res.ifPresent(newTask -> eventBus.publish(new IngressTranslatorCreatedEvent(newTask.getId())));
        return res;
    }


    @Override
    public boolean delete(String id) {
        val res = ingressTranslatorStore.delete(id);
        if (res) {
            eventBus.publish(new IngressTranslatorDeletedEvent(id));
        }
        return res;
    }
}
