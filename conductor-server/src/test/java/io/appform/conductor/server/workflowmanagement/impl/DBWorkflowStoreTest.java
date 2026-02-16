/*
 * Copyright (c) 2023 Santanu Sinha
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

import io.appform.conductor.core.workflowmanagement.impl.DBWorkflowStore;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredTicketState;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredTicketStateTransition;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredWorkflow;
import io.appform.conductor.core.workflowmanagement.impl.models.StoredWorkflowSelectionRule;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link io.appform.conductor.core.workflowmanagement.impl.DBWorkflowStore}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.workflowmanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBWorkflowStoreTest extends AbstractWorkflowStoreTest {

    @Test
    void testBasicCrud(BalancedDBShardingBundle<TestConfig> bundle) {
        val ds = new DBWorkflowStore(
                bundle.createParentObjectDao(StoredWorkflow.class),
                bundle.createRelatedObjectDao(StoredTicketState.class),
                bundle.createRelatedObjectDao(StoredTicketStateTransition.class),
                bundle.createRelatedObjectDao(StoredWorkflowSelectionRule.class));
        checkStoreFunctionality(ds);
    }

}