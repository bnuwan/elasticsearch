/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.security.transport.netty4;

import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.indices.breaker.NoneCircuitBreakerService;
import org.elasticsearch.test.transport.MockTransportService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.ConnectionProfile;
import org.elasticsearch.transport.TcpChannel;
import org.elasticsearch.transport.TcpTransport;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.xpack.security.transport.AbstractSimpleSecurityTransportTestCase;

import java.util.Collections;

public class SimpleSecurityNetty4ServerTransportTests extends AbstractSimpleSecurityTransportTestCase {

    public MockTransportService nettyFromThreadPool(Settings settings, ThreadPool threadPool, final Version version,
                                                    ClusterSettings clusterSettings, boolean doHandshake) {
        NamedWriteableRegistry namedWriteableRegistry = new NamedWriteableRegistry(Collections.emptyList());
        NetworkService networkService = new NetworkService(Collections.emptyList());
        Settings settings1 = Settings.builder()
            .put(settings)
            .put("xpack.security.transport.ssl.enabled", true).build();
        Transport transport = new SecurityNetty4ServerTransport(settings1, version, threadPool,
            networkService, BigArrays.NON_RECYCLING_INSTANCE, namedWriteableRegistry,
            new NoneCircuitBreakerService(), null, createSSLService(settings1)) {

            @Override
            public void executeHandshake(DiscoveryNode node, TcpChannel channel, ConnectionProfile profile,
                                         ActionListener<Version> listener) {
                if (doHandshake) {
                    super.executeHandshake(node, channel, profile, listener);
                } else {
                    listener.onResponse(version.minimumCompatibilityVersion());
                }
            }
        };
        MockTransportService mockTransportService =
            MockTransportService.createNewService(settings, transport, version, threadPool, clusterSettings,
                Collections.emptySet());
        mockTransportService.start();
        return mockTransportService;
    }

    @Override
    protected MockTransportService build(Settings settings, Version version, ClusterSettings clusterSettings, boolean doHandshake) {
        if (TcpTransport.PORT.exists(settings) == false) {
            settings = Settings.builder().put(settings)
                .put(TcpTransport.PORT.getKey(), "0")
                .build();
        }
        MockTransportService transportService = nettyFromThreadPool(settings, threadPool, version, clusterSettings, doHandshake);
        transportService.start();
        return transportService;
    }
}
