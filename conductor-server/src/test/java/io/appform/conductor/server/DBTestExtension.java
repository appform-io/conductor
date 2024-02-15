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

import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.appform.conductor.server.db.InjectedSQLFunctions;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.appform.functionmetrics.FunctionMetricsManager;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.extension.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
@Slf4j
public class DBTestExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private final TestConfig testConfig = new TestConfig();
    private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);
    private final Environment environment = mock(Environment.class);
    private final AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
    private final Bootstrap<?> bootstrap = mock(Bootstrap.class);

    private BalancedDBShardingBundle<TestConfig> bundle;


    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        testConfig.shards.setShards(ImmutableList.of(createConfig("1"), createConfig("2")));
        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.admin()).thenReturn(adminEnvironment);
        when(bootstrap.getHealthCheckRegistry()).thenReturn(new HealthCheckRegistry());
        val entityClassPaths = extensionContext.getTestClass()
                .map(clazz -> clazz.getAnnotation(RelevantDBEntityPackages.class))
                .map(annotation -> Arrays.asList(annotation.value()))
                .orElse(List.of("io.appform.conductor.server"));
        bundle = new BalancedDBShardingBundle<>(entityClassPaths.toArray(new String[0])) {
            @Override
            protected ShardedHibernateFactory getConfig(TestConfig config) {
                return config.shards;
            }
        };
        bundle.initialize(bootstrap);
        bundle.initBundles(bootstrap);
        bundle.runBundles(testConfig, environment);
        bundle.run(testConfig, environment);
        FunctionMetricsManager.initialize("test", SharedMetricRegistries.getOrCreate("test"));
        log.debug("DB sharding bundle initialized...");
        bundle.getSessionFactories().forEach(InjectedSQLFunctions::register);
    }



    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(BalancedDBShardingBundle.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) throws ParameterResolutionException {
        return bundle;
    }

    private static DataSourceFactory createConfig(String dbName) {
//        InjectedSQLFunctions.register(dbName);
        Map<String, String> properties = Maps.newHashMap();
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create");
//        properties.put("hibernate.show_sql", "true");
//        properties.put("hibernate.format_sql", "true");

        DataSourceFactory shard = new DataSourceFactory();
        shard.setDriverClass("org.h2.Driver");
        shard.setUrl("jdbc:h2:mem:" + dbName);
        shard.setValidationQuery("select 1");
        shard.setProperties(properties);

        return shard;
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        bundle.getSessionFactories().forEach(SessionFactory::close);
    }
}
