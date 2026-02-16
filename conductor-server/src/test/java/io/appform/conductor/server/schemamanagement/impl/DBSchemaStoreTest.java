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

package io.appform.conductor.server.schemamanagement.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.conductor.core.schemamanagement.impl.DBSchemaStore;
import io.appform.conductor.server.DBTestExtension;
import io.appform.conductor.server.RelevantDBEntityPackages;
import io.appform.conductor.server.TestConfig;
import io.appform.conductor.core.schemamanagement.impl.models.StoredFieldSchema;
import io.appform.conductor.core.schemamanagement.impl.models.StoredSchemaSummary;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.appform.conductor.core.utils.ConductorServerUtils.configureMapper;

/**
 * Test for {@link io.appform.conductor.core.schemamanagement.impl.DBSchemaStore}
 */
@RelevantDBEntityPackages("io.appform.conductor.server.schemamanagement.impl.models")
@ExtendWith(DBTestExtension.class)
class DBSchemaStoreTest extends AbstractSchemaStoreTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setup() {
        configureMapper(MAPPER);
    }

    @Test
    void test(BalancedDBShardingBundle<TestConfig> bundle) {
        test(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                               bundle.createRelatedObjectDao(StoredFieldSchema.class),
                               MAPPER));
    }

    @Test
    void testFieldCreateOrUpdate(BalancedDBShardingBundle<TestConfig> bundle) {
        testFieldCreateOrUpdate(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                  bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                  MAPPER));
    }

    @Test
    void testExceptionStoreCreateFail(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionStoreCreateFail(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                       bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                       MAPPER));
    }

    @Test
    void testExceptionsOutOfBound(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsOutOfBound(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                   bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                   MAPPER));
    }

    @Test
    void testExceptionsGetSummarySchemaNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsGetSummarySchemaNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                                   bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                                   MAPPER));
    }

    @Test
    void testExceptionsGetSchemaNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsGetSchemaNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                            bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                            MAPPER));
    }

    @Test
    void testExceptionsUpdateDescriptionNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsUpdateDescriptionNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                                    bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                                    MAPPER));
    }

    @Test
    void testExceptionsUpdateStateNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsUpdateStateNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                              bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                              MAPPER));
    }

    @Test
    void testExceptionsAddFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsAddFieldNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                           bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                           MAPPER));
    }

    @Test
    void testExceptionsGetFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsGetFieldNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                           bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                           MAPPER));
    }

    @Test
    void testExceptionsUpdateFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsUpdateFieldNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                              bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                              MAPPER));
    }

    @Test
    void testExceptionsDeleteFieldNullParams(BalancedDBShardingBundle<TestConfig> bundle) {
        testExceptionsDeleteFieldNullParams(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                                              bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                                              MAPPER));
    }

    @Test
    void testExceptions(BalancedDBShardingBundle<TestConfig> bundle) {
        super.testExceptions(new DBSchemaStore(bundle.createParentObjectDao(StoredSchemaSummary.class),
                                               bundle.createRelatedObjectDao(StoredFieldSchema.class),
                                               MAPPER));
    }


}

