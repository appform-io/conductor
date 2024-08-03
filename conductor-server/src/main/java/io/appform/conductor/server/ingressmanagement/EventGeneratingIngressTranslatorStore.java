package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.model.events.impl.ingress.IngressTranslatorCreatedEvent;
import io.appform.conductor.model.events.impl.ingress.IngressTranslatorDeletedEvent;
import io.appform.conductor.model.ingress.IngressTranslator;
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
    public Optional<IngressTranslator> createOrUpdate(IngressTranslator translator) {
        val res =  ingressTranslatorStore.createOrUpdate(translator);
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
