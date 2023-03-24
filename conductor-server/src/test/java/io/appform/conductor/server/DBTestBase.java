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

package io.appform.conductor.server;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for all DB testcases
 */
@Slf4j
public abstract class DBTestBase {

    private final TestConfig testConfig = new TestConfig();
    private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);
    private final Environment environment = mock(Environment.class);
    protected final AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
    private final Bootstrap<?> bootstrap = mock(Bootstrap.class);

    protected BalancedDBShardingBundle<TestConfig> bundle
            = new BalancedDBShardingBundle<TestConfig>("io.appform.conductor.server") {
        @Override
        protected ShardedHibernateFactory getConfig(TestConfig config) {
            return config.shards;
        }
    };

    /**
     * This member will be called by JUnit before every test and will create a pristine in-memory db and setup the store
     */
    @BeforeEach
    public void setupBundle() {
        testConfig.shards.setShards(ImmutableList.of(createConfig("1"), createConfig("2")));
        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.admin()).thenReturn(adminEnvironment);
        when(bootstrap.getHealthCheckRegistry()).thenReturn(new HealthCheckRegistry());
        bundle.initialize(bootstrap);
        bundle.initBundles(bootstrap);
        bundle.runBundles(testConfig, environment);
        bundle.run(testConfig, environment);

        log.debug("DB sharding bundle initialized...");
    }

    private static DataSourceFactory createConfig(String dbName) {
        Map<String, String> properties = Maps.newHashMap();
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create");
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");

        DataSourceFactory shard = new DataSourceFactory();
        shard.setDriverClass("org.h2.Driver");
        shard.setUrl("jdbc:h2:mem:" + dbName);
        shard.setValidationQuery("select 1");
        shard.setProperties(properties);

        return shard;
    }
}