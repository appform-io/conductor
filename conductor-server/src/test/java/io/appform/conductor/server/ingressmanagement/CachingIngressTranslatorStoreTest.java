package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.ingressmanagement.impl.CachingIngressTranslatorStore;
import io.appform.conductor.server.ingressmanagement.impl.DBIngressTranslatorStore;
import io.appform.conductor.server.ingressmanagement.impl.models.StoredIngressTranslator;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@RelevantDBEntityPackages("io.appform.conductor.server.ingressmanagement.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
public class CachingIngressTranslatorStoreTest extends AbstractIngressTranslatorStoreTest {

    @Test
    @SneakyThrows
    void testBasicCrud(BalancedDBShardingBundle<TestConfig> bundle,
                       HazelcastClient hz) {
        val rootIngressTranslatorStore = new DBIngressTranslatorStore(bundle.createParentObjectDao(StoredIngressTranslator.class));
        val ingressTranslatorStore = new CachingIngressTranslatorStore(rootIngressTranslatorStore, hz);
        storeFunctionality(ingressTranslatorStore);
    }
}
