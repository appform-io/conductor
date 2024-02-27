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

import com.google.inject.Stage;
import io.appform.conductor.model.error.ConductorErrorCode;
import io.appform.conductor.model.error.ConductorException;
import io.appform.conductor.server.config.AppConfig;
import io.appform.conductor.server.id.IdGenerator;
import io.appform.conductor.server.ui.HandlebarsViewRenderer;
import io.appform.conductor.server.utils.ConductorServerUtils;
import io.appform.dropwizard.sharding.BalancedDBShardingBundle;
import io.appform.dropwizard.sharding.config.ShardedHibernateFactory;
import io.appform.functionmetrics.FunctionMetricsManager;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.HealthCheckInstaller;
import ru.vyarus.guicey.gsp.ServerPagesBundle;

import java.security.SecureRandom;

/**
 * Main app for conductor
 */
@Slf4j
public class App extends Application<AppConfig> {

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                                               new EnvironmentVariableSubstitutor(true)));
        val dbShardingBundle = new BalancedDBShardingBundle<AppConfig>("io.appform.conductor.server") {
            @Override
            protected ShardedHibernateFactory getConfig(AppConfig appConfig) {
                return appConfig.getDb();
            }
        };
        bootstrap.addBundle(dbShardingBundle);
        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(
                GuiceBundle.builder()
                        .enableAutoConfig("io.appform.conductor.server")
                        .modules(new ConductorModule(dbShardingBundle))
                        .installers(HealthCheckInstaller.class)
                        .bundles(ServerPagesBundle.builder()
                                         .addViewRenderers(new HandlebarsViewRenderer())
                                         .build())
                        .bundles(ServerPagesBundle.app("ui", "/assets/", "/")
                                         .mapViews("/ui")
                                         .requireRenderers("handlebars")
                                         .build())
                        .printDiagnosticInfo()
                        .build(Stage.PRODUCTION));
    }

    @Override
    public void run(AppConfig configuration, Environment environment) {
        val objectMapper = environment.getObjectMapper();
        ConductorServerUtils.configureMapper(objectMapper);
        val serverFactory = (AbstractServerFactory) configuration.getServerFactory();
        serverFactory.setJerseyRootPath("/apis/*");
        serverFactory.setRegisterDefaultExceptionMappers(false);
        IdGenerator.initialize(nodeId());
        FunctionMetricsManager.initialize("io.appform.conductor", environment.metrics());
    }

    public static void main(String[] args) throws Exception {
        val app = new App();
        app.run(args);
    }

    private int nodeId() {
        try {
            val nodeId = SecureRandom.getInstanceStrong().nextInt(1, 9999);
            log.info("Node ID: {}", nodeId);
            return nodeId;
        } catch (Exception e) {
            throw ConductorException.builder()
                    .errorCode(ConductorErrorCode.UNHANDLED_SERVER_ERROR)
                    .build();
        }
    }
}
