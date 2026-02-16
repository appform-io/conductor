package io.appform.conductor.core.ingressmanagement;

import io.appform.conductor.model.actions.Scope;
import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.model.workflow.Template;

import java.util.List;
import java.util.Optional;

public interface IngressTranslatorStore {

    Optional<IngressTranslator> read(final String id);

    List<IngressTranslator> read(final List<String> ids);

    Optional<IngressTranslator> createOrUpdate(final String name, String description,  String ticketIdPath,
                                               Template template, Scope scope);

    Optional<IngressTranslator> update(final String id, String description,
                                       String ticketIdPath, Template template);

    List<IngressTranslator> list(final Scope scope);

    boolean delete(final String id);
}
