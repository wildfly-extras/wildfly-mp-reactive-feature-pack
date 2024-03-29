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

package org.wildfly.extras.quickstart.microprofile.context.propagation;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ContextPropagationQuickstartTestCase.AllowExperimentalAnnotationsSetupTask.class)
public class ContextPropagationQuickstartTestCase {
    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive createWar() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "quickstart-test.war")
                .addPackage(PublisherResource.class.getPackage())
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"))
                .addAsWebInfResource(new File("target/classes/META-INF/persistence.xml"), "classes/META-INF/persistence.xml")
                ;

        war.as(ZipExporter.class).exportTo(new File("target/" + war.getName()), true);

        return war;
    }

    @Test
    public void testPrices() {
        try (Client client = ClientBuilder.newClient()) {
            // Check we don't have any prices
            List<Price> prices = client.target(getAddress("prices/all")).request(MediaType.APPLICATION_JSON).get(new GenericType<>() {
            });
            Assert.assertTrue(prices.isEmpty());

            // Stream the prices
            WebTarget target = client.target(getAddress("prices"));
            List<Double> received = new CopyOnWriteArrayList<>();
            SseEventSource source = SseEventSource.target(target).build();
            source.register(inboundSseEvent -> received.add(Double.valueOf(inboundSseEvent.readData())));
            source.open();

            // Send the prices
            Price p1 = new Price();
            p1.setValue(1.0);
            Price p2 = new Price();
            p2.setValue(4.0);
            Price p3 = new Price();
            p3.setValue(2.0);
            final WebTarget postTarget = client.target(getAddress(""));
            post(postTarget, p1);
            post(postTarget, p2);
            post(postTarget, p3);

            await().atMost(100000, MILLISECONDS).until(() -> received.size() == 3);
            source.close();

            Assert.assertTrue(received.contains(p1.getValue()));
            Assert.assertTrue(received.contains(p2.getValue()));
            Assert.assertTrue(received.contains(p3.getValue()));

            prices = client.target(getAddress("prices/all")).request().get(new GenericType<>() {});
            Assert.assertEquals(3, prices.size());
            Assert.assertEquals(p1.getValue(), prices.get(0).getValue());
            Assert.assertEquals(p2.getValue(), prices.get(1).getValue());
            Assert.assertEquals(p3.getValue(), prices.get(2).getValue());
        }
    }

    @Test
    public void testNames() {
        // Check we don't have any names
        try (Client client = ClientBuilder.newClient()) {
            final GenericType<List<Name>> genericType = new GenericType<>(){};
            List<Name> names = client.target(getAddress("names/all")).request().get(genericType);
            Assert.assertTrue(names.isEmpty());

            List<Name> tcNames = client.target(getAddress("names-tc")).request().get(genericType);
            Assert.assertEquals(3, tcNames.size());

            List<Name> meNames = client.target(getAddress("names-me")).request().get(genericType);
            Assert.assertEquals(3, meNames.size());

            names = client.target(getAddress("names/all")).request().get(genericType);
            Assert.assertEquals(6, names.size());

            Assert.assertEquals(tcNames.get(0), names.get(0));
            Assert.assertEquals(tcNames.get(1), names.get(1));
            Assert.assertEquals(tcNames.get(2), names.get(2));
            Assert.assertEquals(meNames.get(0), names.get(3));
            Assert.assertEquals(meNames.get(1), names.get(4));
            Assert.assertEquals(meNames.get(2), names.get(5));
        }
    }


    private String getAddress(String path) {
        return url.toExternalForm() + path;
    }

    private static void post(final WebTarget target, final Price entity) {
        try (Response response = target.request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .post(Entity.json(entity))) {
            Assert.assertEquals(204, response.getStatus());
        }
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
}
