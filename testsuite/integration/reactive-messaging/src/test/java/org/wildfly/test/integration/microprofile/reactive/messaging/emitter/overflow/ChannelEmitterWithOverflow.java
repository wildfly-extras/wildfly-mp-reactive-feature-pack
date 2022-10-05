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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;


/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class ChannelEmitterWithOverflow {

    @Inject
    @Channel("sink")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER)
    Emitter<String> emitter;

    private Emitter<String> emitterForSink2;
    private Emitter<String> emitterForSink1;

    @Inject
    public void setEmitter(@Channel("sink-1") Emitter<String> sink1,
                           @Channel("sink-2") @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 4) Emitter<String> sink2) {
        this.emitterForSink1 = sink1;
        this.emitterForSink2 = sink2;
    }

    private List<String> list = new CopyOnWriteArrayList<>();
    private List<String> sink1 = new CopyOnWriteArrayList<>();
    private List<String> sink2 = new CopyOnWriteArrayList<>();

    public void run() {
        emitter.send("a");
        emitter.send("b");
        emitter.send("c").toCompletableFuture().join();
        emitter.complete();
        emitterForSink1.send("a1");
        emitterForSink1.send("b1").toCompletableFuture().join();
        emitterForSink1.send("c1");
        emitterForSink1.complete();
        emitterForSink2.send("a2").toCompletableFuture().join();
        emitterForSink2.send("b2");
        emitterForSink2.send("c2");
        emitterForSink2.complete();
    }

    @Incoming("sink")
    public void consume(String s) {
        list.add(s);
    }

    @Incoming("sink-1")
    public void consume1(String s) {
        sink1.add(s);
    }

    @Incoming("sink-2")
    public void consume2(String s) {
        sink2.add(s);
    }

    public List<String> list() {
        return list;
    }

    public List<String> sink1() {
        return sink1;
    }

    public List<String> sink2() {
        return sink2;
    }
}
