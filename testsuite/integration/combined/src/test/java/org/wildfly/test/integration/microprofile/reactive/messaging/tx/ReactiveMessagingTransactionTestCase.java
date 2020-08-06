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

package org.wildfly.test.integration.microprofile.reactive.messaging.tx;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.context.propagation.rest.tx.TxContextPropagationClientTestCase;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ApplicationScoped
public class ReactiveMessagingTransactionTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    Bean bean;

    @Inject
    @Any // TODO why is this needed here and not in TxContextPropagationEndpoint?
    ManagedExecutor executor;

    @Inject
    TransactionalBean txBean;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rx-messaging.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingTransactionTestCase.class.getPackage())
                .addAsWebInfResource(TxContextPropagationClientTestCase.class.getPackage(), "persistence.xml", "classes/META-INF/persistence.xml")
                .addClass(TimeoutUtil.class);
        return webArchive;
    }

    @Outgoing("source")
    public PublisherBuilder<String> source() {
        txBean.checkValues(Collections.emptySet());
        return ReactiveStreams.of("hello", "reactive", "messaging");
    }

    @Incoming("source")
    @Outgoing("sink")
    public CompletionStage<String> store(String payload) {
        // Use the executor to make sure it is on a separate thread
        CompletableFuture<String> ret = executor.completedFuture(payload);
        return ret.thenApplyAsync(v -> {
            if (v.equals("reactive")) {
                // Add a sleep here to make sure the calling method has returned
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e){
                    throw new RuntimeException(e);
                }
                txBean.storeValue(v);
            }
            return v;
        });
    }

    @Incoming("sink")
    public void sink(String word) {
        bean.addWord(word);
    }

    @Test
    public void test() throws InterruptedException {
        boolean wait = bean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Timed out", wait);
        Assert.assertEquals("hello reactive messaging", bean.getPhrase());

        // Check the data was stored
        txBean.checkValues(Collections.singleton("reactive"));
    }
}
