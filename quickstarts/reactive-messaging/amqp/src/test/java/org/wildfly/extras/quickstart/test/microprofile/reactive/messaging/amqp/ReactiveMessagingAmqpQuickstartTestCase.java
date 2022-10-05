/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extras.quickstart.test.microprofile.reactive.messaging.amqp;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.wildfly.extras.quickstart.microprofile.reactive.messaging.PriceConverter;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
        ReactiveMessagingAmqpQuickstartTestCase.KafkaBootstrapServerSetupTask.class,
        ReactiveMessagingAmqpQuickstartTestCase.AllowExperimentalAnnotationsSetupTask.class})
@Ignore("https://github.com/wildfly-extras/wildfly-mp-reactive-feature-pack/issues/41")
public class ReactiveMessagingAmqpQuickstartTestCase {
    private static final int AMQP_PORT = 5672;
    private static final GenericContainer AMQP = new GenericContainer("vromero/activemq-artemis:2.8.0-alpine")
            .withEnv("ARTEMIS_USERNAME", "wildfly")
            .withEnv("ARTEMIS_PASSWORD", "wildfly")
            .withExposedPorts(8161, 61616, AMQP_PORT);

    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive createWar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "quickstart-test.war")
                .addPackage(PriceConverter.class.getPackage())
                .add(new FileAsset(new File("../core/src/main/webapp/index.html")), "/index.html")
                .addAsWebInfResource(new File("../core/src/main/webapp/WEB-INF/web.xml"))
                .addAsWebInfResource(new File("target/classes/META-INF/microprofile-config.properties"), "classes/META-INF/microprofile-config.properties")
                ;

        war.as(ZipExporter.class).exportTo(new File("target/" + war.getName()), true);

        return war;
    }

    @Test
    public void testPricesEventStream() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url.toExternalForm() + "rest/prices");

        List<Double> received = new CopyOnWriteArrayList<>();

        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> received.add(Double.valueOf(inboundSseEvent.readData())));
        source.open();

        long end = System.currentTimeMillis() + TimeoutUtil.adjust(20000);
        while (System.currentTimeMillis() < end && received.size() < 3) {
            Thread.sleep(100);
        }
        source.close();
        int size = received.size();
        Assert.assertTrue(size + " entries, expected at least 3", size >= 3);
    }

    public static class AllowExperimentalAnnotationsSetupTask extends CLIServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            this.builder.node(containerId)
                    .setup("/system-property=jboss.as.reactive.messaging.experimental:add(value=true)")
                    .teardown("/system-property=amqp-port:remove");
            super.setup(managementClient, containerId);
        }
    }

    public static class KafkaBootstrapServerSetupTask extends CLIServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            AMQP.start();
            Integer amqpPort = AMQP.getMappedPort(AMQP_PORT);
            this.builder.node("default")
                    .setup("/system-property=amqp-port:add(value=" + amqpPort + ")")
                    .teardown("/system-property=amqp-port:remove");
            super.setup(managementClient, containerId);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try {
                super.tearDown(managementClient, containerId);
            } finally {
                AMQP.stop();
            }
        }
    }

}
