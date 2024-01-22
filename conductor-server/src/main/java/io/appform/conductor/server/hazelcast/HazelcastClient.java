/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appform.conductor.server.hazelcast;

import com.google.common.base.Strings;
import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.appform.conductor.server.config.hz.*;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.jetty.server.Server;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.spi.CachingProvider;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * User: Santanu Sinha (santanu.sinha@flipkart.com)
 * Date: 15/03/14
 * Time: 10:01 PM
 */
@Order(10)
@Singleton
@Slf4j
public class HazelcastClient implements Managed, ServerLifecycleListener {
    public static final String HEALTHCHECK_MAP = "healthCheck";

    private HazelcastInstance hazelcast = null;
    private final Config hazelcastConfig;

    @Getter
    private CacheManager cacheManager;


    @Inject
    public HazelcastClient(ClusterConfig clusterConfig) {
        val hzConfig = new Config();
        hzConfig.setClusterName(clusterConfig.getName());
        clusterConfig.getDiscovery()
                .accept(new ClusterDiscoveryConfigVisitor<Void>() {
                    @Override
                    public Void visit(AwsClusterDiscoveryConfig awsClusterDiscoveryConfig) {
                        NetworkConfig hazelcastConfigNetworkConfig = hzConfig.getNetworkConfig();
                        JoinConfig hazelcastConfigNetworkConfigJoin = hazelcastConfigNetworkConfig.getJoin();
                        hazelcastConfigNetworkConfigJoin.getTcpIpConfig()
                                .setEnabled(false);
                        hazelcastConfigNetworkConfigJoin.getMulticastConfig()
                                .setEnabled(false);
                        AwsConfig awsConfig = new AwsConfig();

                        if (!Strings.isNullOrEmpty(awsClusterDiscoveryConfig.getServiceName())) {
                            awsConfig.setProperty("service-name", awsClusterDiscoveryConfig.getServiceName());
                        }
                        if (!Strings.isNullOrEmpty(awsClusterDiscoveryConfig.getAccessKey())) {
                            awsConfig.setProperty("access-key", awsClusterDiscoveryConfig.getAccessKey());
                        }
                        if (!Strings.isNullOrEmpty(awsClusterDiscoveryConfig.getSecretKey())) {
                            awsConfig.setProperty("secret-key", awsClusterDiscoveryConfig.getSecretKey());
                        }
                        if (!Strings.isNullOrEmpty(awsClusterDiscoveryConfig.getIamRole())) {
                            awsConfig.setProperty("iam-role", awsClusterDiscoveryConfig.getIamRole());
                        }
                        if (!Strings.isNullOrEmpty(awsClusterDiscoveryConfig.getRegion())) {
                            awsConfig.setProperty("region", awsClusterDiscoveryConfig.getRegion());
                        }
                        if (!Strings.isNullOrEmpty(awsClusterDiscoveryConfig.getHostHeader())) {
                            awsConfig.setProperty("host-header", awsClusterDiscoveryConfig.getHostHeader());
                        }
                        if (!Strings.isNullOrEmpty(awsClusterDiscoveryConfig.getSecurityGroupName())) {
                            awsConfig.setProperty("security-group-name",
                                                  awsClusterDiscoveryConfig.getSecurityGroupName());
                        }
                        if (awsClusterDiscoveryConfig.getOpTimeoutSeconds() > 0) {
                            awsConfig.setProperty("connection-timeout-seconds",
                                                  Integer.toString(awsClusterDiscoveryConfig.getOpTimeoutSeconds()));
                            awsConfig.setProperty("read-timeout-seconds",
                                                  Integer.toString(awsClusterDiscoveryConfig.getOpTimeoutSeconds()));
                        }
                        if (awsClusterDiscoveryConfig.isExternalClient()) {
                            awsConfig.setProperty("use-public-ip", Boolean.TRUE.toString());
                        }
                        hazelcastConfigNetworkConfigJoin.setAwsConfig(awsConfig);
                        hazelcastConfigNetworkConfigJoin.getAwsConfig()
                                .setEnabled(true);
                        return null;
                    }

                    @Override
                    public Void visit(AwsECSDiscoveryConfig awsECSDiscoveryConfig) {
                        NetworkConfig hazelcastConfigNetworkConfig = hzConfig.getNetworkConfig();
//                JoinConfig hazelcastConfigNetworkConfigJoin = hazelcastConfigNetworkConfig.getJoin();
                        hazelcastConfigNetworkConfig.getJoin()
                                .getMulticastConfig()
                                .setEnabled(false);

                        hazelcastConfigNetworkConfig.getInterfaces()
                                .setEnabled(true)
                                .addInterface(awsECSDiscoveryConfig.getNetwork());

                        AwsConfig awsConfig = new AwsConfig();
                        awsConfig.setEnabled(true);
                        if (!Strings.isNullOrEmpty(awsECSDiscoveryConfig.getAccessKey())) {
                            awsConfig.setProperty("access-key", awsECSDiscoveryConfig.getAccessKey());
                        }
                        if (!Strings.isNullOrEmpty(awsECSDiscoveryConfig.getSecretKey())) {
                            awsConfig.setProperty("secret-key", awsECSDiscoveryConfig.getSecretKey());
                        }
                        if (!Strings.isNullOrEmpty(awsECSDiscoveryConfig.getRegion())) {
                            awsConfig.setProperty("region", awsECSDiscoveryConfig.getRegion());
                        }
                        if (!Strings.isNullOrEmpty(awsECSDiscoveryConfig.getCluster())) {
                            awsConfig.setProperty("cluster", awsECSDiscoveryConfig.getCluster());
                        }
                        if (!Strings.isNullOrEmpty(awsECSDiscoveryConfig.getFamily())) {
                            awsConfig.setProperty("family", awsECSDiscoveryConfig.getFamily());
                        }
                        if (!Strings.isNullOrEmpty(awsECSDiscoveryConfig.getServiceName())) {
                            awsConfig.setProperty("service-name", awsECSDiscoveryConfig.getServiceName());
                        }
                        if (!Strings.isNullOrEmpty(awsECSDiscoveryConfig.getHostHeader())) {
                            awsConfig.setProperty("host-header", awsECSDiscoveryConfig.getHostHeader());
                        }
                        if (awsECSDiscoveryConfig.getOpTimeoutSeconds() > 0) {
                            awsConfig.setProperty("connection-timeout-seconds",
                                                  Integer.toString(awsECSDiscoveryConfig.getOpTimeoutSeconds()));
                            awsConfig.setProperty("read-timeout-seconds",
                                                  Integer.toString(awsECSDiscoveryConfig.getOpTimeoutSeconds()));
                        }
                        if (awsECSDiscoveryConfig.isExternalClient()) {
                            awsConfig.setProperty("use-public-ip", Boolean.TRUE.toString());
                        }
                        return null;
                    }

                    @Override
                    public Void visit(KubernetesClusterDiscoveryConfig kubernetesClusterDiscoveryConfig) {
                        JoinConfig kbConfig = hzConfig.getNetworkConfig().getJoin();
                        kbConfig.getMulticastConfig()
                                .setEnabled(!kubernetesClusterDiscoveryConfig.isDisableMulticast());
                        kbConfig.getKubernetesConfig().setEnabled(true);
                        return null;
                    }

                    @Override
                    @SneakyThrows
                    public Void visit(SimpleClusterDiscoveryConfig simpleClusterDiscoveryConfig) {
                        final String hostName = InetAddress.getLocalHost()
                                .getCanonicalHostName();
                        hzConfig.setInstanceName(String.format("foxtrot-%s-%d", hostName, System.currentTimeMillis()));
                        if (simpleClusterDiscoveryConfig.isDisableMulticast()) {
                            hzConfig.getNetworkConfig()
                                    .getJoin()
                                    .getMulticastConfig()
                                    .setEnabled(false);
                            for (String member : simpleClusterDiscoveryConfig.getMembers()) {
                                hzConfig.getNetworkConfig()
                                        .getJoin()
                                        .getTcpIpConfig()
                                        .addMember(member);
                            }
                            hzConfig.getNetworkConfig()
                                    .getJoin()
                                    .getTcpIpConfig()
                                    .setEnabled(true);
                        }
                        return null;
                    }
                });
        this.hazelcastConfig = hzConfig;
    }

    @Override
    public void start() throws Exception {
        if (null == hazelcast) {
            log.info("Starting Hazelcast Instance");
            configureHealthcheck();
            hazelcast = Hazelcast.newHazelcastInstance(hazelcastConfig);
            CachingProvider cachingProvider =
                    Caching.getCachingProvider(HazelcastCachingProvider.MEMBER_CACHING_PROVIDER);
            cacheManager = cachingProvider.getCacheManager(null,
                                                           null,
                                                           HazelcastCachingProvider.propertiesByInstanceName(
                                                                   hazelcastConfig.getInstanceName()));
        }
        else {
            log.info("Skipped starting HZ as this was already started");
        }
        log.info("Started Hazelcast Instance");
    }


    public <K, V> Provider<Cache<K, V>> consistentCache(
            String name, Consumer<Cache<K, V>> initializer) {
        return configuredCache(name,
                               new MutableConfiguration<K, V>()
                                       .setExpiryPolicyFactory(EternalExpiryPolicy::new)
                                       .setReadThrough(false)
                                       .setWriteThrough(false)
                                       .setStatisticsEnabled(true),
                               initializer);
    }

    public <K, V> Provider<Cache<K, V>> loadingCache(
            String name, CacheLoader<K, V> loader) {
        return loadingCache(name, loader, cache -> {});
    }

    public <K, V> Provider<Cache<K, V>> loadingCache(
            String name, CacheLoader<K, V> loader, Consumer<Cache<K, V>> initializer) {
        return configuredCache(name,
                               new MutableConfiguration<K, V>()
                                       .setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> new TouchedExpiryPolicy(
                                               Duration.TEN_MINUTES))
                                       .setReadThrough(true)
                                       .setCacheLoaderFactory((Factory<CacheLoader<K, V>>) () -> loader)
                                       .setWriteThrough(false)
                                       .setStatisticsEnabled(true),
                               initializer);
    }

    public <K, V> Provider<Cache<K, V>> configuredCache(
            String name,
            Configuration<K, V> config,
            Consumer<Cache<K, V>> initializer) {
        return () -> Objects.requireNonNullElseGet(cacheManager.getCache(name),
                                                   () -> {
                                                       val cache = cacheManager.createCache(name, config);
                                                       initializer.accept(cache);
                                                       log.info("Created cache {}", name);
                                                       return cache;
                                                   });
    }

    private void configureHealthcheck() {
        MapConfig mapConfig = new MapConfig(HEALTHCHECK_MAP);
        mapConfig.setInMemoryFormat(InMemoryFormat.BINARY);
        hazelcastConfig.addMapConfig(mapConfig);
    }

    @Override
    public void stop() throws Exception {
        StreamSupport.stream(cacheManager.getCacheNames().spliterator(), false)
                .forEach(cacheName -> {
                    cacheManager.getCache(cacheName).close();
                    log.info("Closed cache {}", cacheName);
                });
        hazelcast.shutdown();
    }

    @Override
    public void serverStarted(Server server) {

    }
}
