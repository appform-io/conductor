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

package io.appform.conductor.server.usermanagement.impl;

import io.appform.conductor.model.usermgmt.GroupType;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.user.usermanagement.impl.DBGroupStore;
import io.appform.conductor.user.usermanagement.impl.models.StoredGroup;
import io.appform.conductor.user.usermanagement.impl.models.StoredGroupUserMapping;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.usermanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBGroupStoreTest {

    @Test
    void testCreate(BalancedDBShardingBundle<TestConfig> bundle) {
        val groupStore = new DBGroupStore(createRealGroupDao(bundle), createRealGroupUserMappingDao(bundle));
        val group = groupStore.create("Test", "Test group", GroupType.MANUALLY_ASSIGNED, Set.of()).orElse(null);
        assertNotNull(group);
    }

    @Test
    void testCreateAssociation(BalancedDBShardingBundle<TestConfig> bundle) {
        val groupStore = new DBGroupStore(createRealGroupDao(bundle), createRealGroupUserMappingDao(bundle));
        groupStore.create("Test", "Test Group", GroupType.MANUALLY_ASSIGNED, Set.of());
        assertTrue(groupStore.addUserToGroup("test", "test-user"));
        assertEquals(1, groupStore.findUsersForGroup("test", 0, Integer.MAX_VALUE).size());
        assertTrue(groupStore.removeUserFromGroup("test", "test-user"));
        assertEquals(0, groupStore.findUsersForGroup("test", 0, Integer.MAX_VALUE).size());
        assertTrue(groupStore.addUserToGroup("test", "test-user"));
        assertEquals(1, groupStore.findUsersForGroup("test", 0, Integer.MAX_VALUE).size());
    }

    private LookupDao<StoredGroup> createRealGroupDao(BalancedDBShardingBundle<TestConfig> bundle) {
        return bundle.createParentObjectDao(StoredGroup.class);
    }

    private RelationalDao<StoredGroupUserMapping> createRealGroupUserMappingDao(BalancedDBShardingBundle<TestConfig> bundle) {
        return bundle.createRelatedObjectDao(StoredGroupUserMapping.class);
    }
}