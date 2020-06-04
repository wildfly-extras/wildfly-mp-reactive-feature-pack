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

package org.wildfly.test.integration.microprofile.reactive.messaging.emitter.overflow;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
public class EmitterWithOverflowTestCase {

    @Inject
    ChannelEmitterWithOverflow streamEmitter;

    @Deployment
    public static Archive<?> getDeployment(){
        final WebArchive war = create(WebArchive.class, "messaging-rso.war")
                .addPackage(EmitterWithOverflowTestCase.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        return war;
    }

    @Test
    public void testStreamEmitter() {
        streamEmitter.run();
        List<String> list = streamEmitter.list();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> sink1 = streamEmitter.sink1();
        assertEquals(3, sink1.size());
        assertEquals("a1", sink1.get(0));
        assertEquals("b1", sink1.get(1));
        assertEquals("c1", sink1.get(2));

        List<String> sink2 = streamEmitter.sink2();
        assertEquals(3, sink2.size());
        assertEquals("a2", sink2.get(0));
        assertEquals("b2", sink2.get(1));
        assertEquals("c2", sink2.get(2));
    }

}
