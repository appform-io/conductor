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

import io.appform.conductor.core.config.hz.ClusterConfig;
import io.appform.conductor.core.config.hz.SimpleClusterDiscoveryConfig;
import io.appform.conductor.core.hazelcast.HazelcastClient;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.extension.*;

import java.util.stream.StreamSupport;

/**
 *
 */
@Slf4j
public class HazelcastTestExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback, ParameterResolver {
    private HazelcastClient hazelcast;

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(HazelcastClient.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext,
            ExtensionContext extensionContext) throws ParameterResolutionException {
        return hazelcast;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        val config = new ClusterConfig().setName("conductor").setDiscovery(new SimpleClusterDiscoveryConfig());
        hazelcast = new HazelcastClient(config);
        hazelcast.start();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        hazelcast.stop();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        StreamSupport.stream(hazelcast.getCacheManager().getCacheNames().spliterator(), false)
                .forEach(cacheName -> hazelcast.getCacheManager().getCache(cacheName).clear());
    }
}
