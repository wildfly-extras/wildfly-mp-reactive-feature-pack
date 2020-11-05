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

package org.wildfly.test.integration.microprofile.reactive.messaging.emitter.simple;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.messaging.AllowExperimentalAnnotationsSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(AllowExperimentalAnnotationsSetupTask.class)
public class SimpleTestCase {

    @Deployment
    public static Archive<?> getDeployment(){
        final WebArchive war = create(WebArchive.class, "messaging-rso.war")
                .addClasses(SimpleTestCase.class, StreamConsumer.class, StreamEmitter.class, SimpleBean.class)
                .addClass(AllowExperimentalAnnotationsSetupTask.class)
                .addClass(CLIServerSetupTask.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        return war;
    }

    @Inject
    StreamConsumer streamConsumer;

    @Inject
    StreamEmitter streamEmitter;

     @Test
    public void testSimpleBean() {
        assertEquals(4, SimpleBean.RESULT.size());
        assertTrue(SimpleBean.RESULT.contains("HELLO"));
        assertTrue(SimpleBean.RESULT.contains("SMALLRYE"));
        assertTrue(SimpleBean.RESULT.contains("REACTIVE"));
        assertTrue(SimpleBean.RESULT.contains("MESSAGE"));
    }

    @Test
    public void testStreamInject() {
        List<String> consumed = streamConsumer.consume();
        assertEquals(5, consumed.size());
        assertEquals("hello", consumed.get(0));
        assertEquals("with", consumed.get(1));
        assertEquals("SmallRye", consumed.get(2));
        assertEquals("reactive", consumed.get(3));
        assertEquals("message", consumed.get(4));
    }

    @Test
    public void testStreamEmitter() {
        streamEmitter.run();
        List<String> list = streamEmitter.list();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

}
