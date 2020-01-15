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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.OnOverflow;
import io.smallrye.reactive.messaging.annotations.Stream;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class StreamEmitterWithOverflow {

    @Inject
    @Channel("sink")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER)
    Emitter<String> emitter;

    private Emitter<String> emitterForSink2;
    private Emitter<String> emitterForSink1;

    @Inject
    public void setEmitter(@Stream("sink-1") Emitter<String> sink1,
            @Stream("sink-2") @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 2) Emitter<String> sink2) {
        this.emitterForSink1 = sink1;
        this.emitterForSink2 = sink2;
    }

    private List<String> list = new CopyOnWriteArrayList<>();
    private List<String> sink1 = new CopyOnWriteArrayList<>();
    private List<String> sink2 = new CopyOnWriteArrayList<>();

    public void run() {
        emitter.send("a").send("b").send("c").complete();
        emitterForSink1.send("a1").send("b1").send("c1").complete();
        emitterForSink2.send("a2").send("b2").send("c2").complete();
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
