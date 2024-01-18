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

package io.appform.conductor.server.schemamanagement.impl;

import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.HazelcastTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.server.hazelcast.HazelcastClient;
import io.appform.conductor.server.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.server.schemamanagement.impl.models.StoredSchemaSummary;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Consumer;

/**
 *
 */
@RelevantDBEntityPackages("io.appform.conductor.server.schemamanagement.impl.models")
@ExtendWith({DBTestExtension.class, HazelcastTestExtension.class})
class CachingSchemaStoreTest extends AbstractSchemaStoreTest {

    @Test
    void test(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::test);
    }

    @Test
    void testFieldCreateOrUpdate(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testFieldCreateOrUpdate);
    }

    @Test
    void testExceptionStoreCreateFail(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionStoreCreateFail);
    }

    @Test
    void testExceptionsOutOfBound(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsOutOfBound);
    }

    @Test
    void testExceptionsGetSummarySchemaNullParams(
            BalancedDBShardingBundle<TestConfig> bundle,
            HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsGetSummarySchemaNullParams);
    }

    @Test
    void testExceptionsGetSchemaNullParams(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsGetSchemaNullParams);
    }

    @Test
    void testExceptionsUpdateDescriptionNullParams(
            BalancedDBShardingBundle<TestConfig> bundle,
            HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsUpdateDescriptionNullParams);
    }

    @Test
    void testExceptionsUpdateStateNullParams(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsUpdateStateNullParams);
    }

    @Test
    void testExceptionsAddFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsAddFieldNullParams);
    }

    @Test
    void testExceptionsGetFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsGetFieldNullParams);
    }

    @Test
    void testExceptionsUpdateFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsUpdateFieldNullParams);
    }

    @Test
    void testExceptionsDeleteFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptionsDeleteFieldNullParams);
    }

    @Test
    void testExceptions(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        runTest(bundle, hazelcast, super::testExceptions);
    }

    private void runTest(
            BalancedDBShardingBundle<TestConfig> bundle,
            HazelcastClient hazelcast,
            Consumer<SchemaStore> runner) {
        runner.accept(createStore(bundle, hazelcast));
    }

    @SneakyThrows
    private CachingSchemaStore createStore(BalancedDBShardingBundle<TestConfig> bundle, HazelcastClient hazelcast) {
        return new CachingSchemaStore(
                new DBSchemaStore(
                        bundle.createParentObjectDao(StoredSchemaSummary.class),
                        bundle.createRelatedObjectDao(StoredFieldSchema.class),
                        MAPPER),
                hazelcast);
    }
}