package io.appform.conductor.server.ingressmanagement.impl;

import io.appform.conductor.model.ingress.IngressTranslator;
import io.appform.conductor.server.ConductorModule;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.ingressmanagement.IngressTranslatorStore;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.cache.Cache;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
@Slf4j
public class CachingIngressTranslatorStore implements IngressTranslatorStore {
    private final IngressTranslatorStore root;
    private final Provider<Cache<String, IngressTranslator>> ingressTranslatorCacheProvider;

    @Inject
    public CachingIngressTranslatorStore(
            @Named(ConductorModule.ROOT_IMPLEMENTATION_NAME) final IngressTranslatorStore root,
            final HazelcastClient hazelcastClient) {
        this.root = root;
        this.ingressTranslatorCacheProvider = hazelcastClient.loadingCache(
                getClass().getSimpleName() + "-ingress-translator",
                new CacheLoader<>() {
                    @Override
                    public IngressTranslator load(String key) throws CacheLoaderException {
                        log.debug("Loading data for ingress translator {}", key);
                        return root.read(key).orElse(null);
                    }

                    @Override
                    public Map<String, IngressTranslator> loadAll(Iterable<? extends String> keys) throws CacheLoaderException {
                        val ids = StreamSupport.stream(keys.spliterator(), false)
                                .map(String.class::cast)
                                .toList();
                        log.debug("Loading data for ingress translators: {}", ids);
                        return root.read(ids)
                                .stream()
                                .collect(Collectors.toUnmodifiableMap(IngressTranslator::getId, Function.identity()));
                    }
                });
    }

    @Override
    public Optional<IngressTranslator> read(String id) {
        return Optional.ofNullable(ingressTranslatorCacheProvider.get().get(id));
    }

    @Override
    public List<IngressTranslator> read(List<String> ids) {
        return List.copyOf(ingressTranslatorCacheProvider.get().getAll(Set.copyOf(ids)).values());
    }

    @Override
    public Optional<IngressTranslator> createOrUpdate(IngressTranslator translator) {
        return root.createOrUpdate(translator)
                .flatMap(this::refreshData);
    }

    @Override
    public boolean delete(String id) {
        val status = root.delete(id);
        if (status) {
            log.debug("Removing data for deleted ingress translator: {}", id);
            ingressTranslatorCacheProvider.get().remove(id);
        }
        return status;
    }

    private Optional<IngressTranslator> refreshData(IngressTranslator ingressTranslator) {
        String id = ingressTranslator.getId();
        Objects.requireNonNull(id);
        val cache = ingressTranslatorCacheProvider.get();
        log.debug("Removing data for ingress translator: {}", id);
        cache.remove(id); //Let it load organically
        return read(id);
    }
}
