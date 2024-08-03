package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.model.ingress.IngressTranslator;

import java.util.List;
import java.util.Optional;

public interface IngressTranslatorStore {

    Optional<IngressTranslator> read(final String id);

    List<IngressTranslator> read(final List<String> ids);

    Optional<IngressTranslator> createOrUpdate(final IngressTranslator translator);

    boolean delete(final String id);
}
