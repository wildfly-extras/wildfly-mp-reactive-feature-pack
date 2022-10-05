/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extras.quickstart.microprofile.reactive.messaging;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Class simulating external data being made available asynchronously.
 * In the real world this could be for example a REST client making
 * asynchronous calls. The implementation details of this class are
 * not important. The key point is that the {@link #getNextValue()}
 * method returns a CompletionStage which returns an int. These
 * ints are 'emitted' every five seconds.
 *
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class MockExternalAsyncResource {
    private static final int TICK = 5000;

    private Random random = new Random();
    private ScheduledExecutorService delayedExecutor = Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());
    private final AtomicInteger count = new AtomicInteger(0);
    private long last = System.currentTimeMillis();

    @PreDestroy
    public void stop() {
        delayedExecutor.shutdown();
    }

    public CompletionStage<Integer> getNextValue() {
        synchronized (this) {
            CompletableFuture<Integer> cf = new CompletableFuture<>();
            long now = System.currentTimeMillis();
            long next = TICK + last;
            long delay = next - now;
            last = next;
            delayedExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    cf.complete(random.nextInt(100));
                }
            }, delay, TimeUnit.MILLISECONDS);
            return cf;
        }
    }

}
