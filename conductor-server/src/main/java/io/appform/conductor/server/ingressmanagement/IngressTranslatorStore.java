package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.model.workflow.Template;

import java.util.List;
import java.util.Optional;

public interface IngressTranslatorStore {

    Optional<IngressTranslator> read(final String id);

    List<IngressTranslator> read(final List<String> ids);

    Optional<IngressTranslator> createOrUpdate(final String name, String description, Template template);

    Optional<IngressTranslator> update(final String id, String description, Template template);

    List<IngressTranslator> list();

    boolean delete(final String id);
}
