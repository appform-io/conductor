package io.appform.conductor.server.ingressmanagement;

import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.ingressmanagement.impl.DBIngressTranslatorStore;
import io.appform.conductor.server.ingressmanagement.impl.models.StoredIngressTranslator;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;



@RelevantDBEntityPackages("io.appform.conductor.server.ingressmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
public class DBIngressTranslatorStoreTest extends AbstractIngressTranslatorStoreTest {


    @Test
    void testBasicCrud(BalancedDBShardingBundle<TestConfig> bundle) {
        val ingressTranslatorStore = new DBIngressTranslatorStore(bundle.createParentObjectDao(StoredIngressTranslator.class));
        storeFunctionality(ingressTranslatorStore);
    }
}
