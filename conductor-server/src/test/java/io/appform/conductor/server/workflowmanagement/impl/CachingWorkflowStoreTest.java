/*
 * Copyright (c) 2024 santanu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.workflowmanagement.impl;

import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.config.hz.ClusterConfig;
import io.appform.conductor.server.config.hz.SimpleClusterDiscoveryConfig;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.server.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.SneakyThrows;
import lombok.val;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.workflowmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class CachingWorkflowStoreTest extends AbstractWorkflowStoreTest {

    @Test
    @SneakyThrows
    void testBasicCrud(BalancedDBShardingBundle<TestConfig> bundle) {
        val config = new ClusterConfig().setName("conductor").setDiscovery(new SimpleClusterDiscoveryConfig());
        val hz = new HazelcastClient(config);
        val ds = new CachingWorkflowStore(new DBWorkflowStore(
                bundle.createParentObjectDao(StoredWorkflow.class),
                bundle.createRelatedObjectDao(StoredTicketState.class),
                bundle.createRelatedObjectDao(StoredTicketStateTransition.class),
                bundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class)),
                hz);
        hz.start();
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                        .until(ds::isInitialized);
        checkStoreFunctionality(ds);
    }

}