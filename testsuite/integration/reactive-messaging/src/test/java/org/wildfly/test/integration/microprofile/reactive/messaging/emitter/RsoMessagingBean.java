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

package org.wildfly.test.integration.microprofile.reactive.messaging.emitter;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class RsoMessagingBean {

    // An Emitter 'bridges the imperative and reactive worlds'
    // It is incubated in SmallRye but will hopefully make it to the spec one day
    // https://smallrye.io/smallrye-reactive-messaging/#_channel
    // The EmitterImpl wraps a bunch of RxJava stuff, so it seems this is not possible really to do do with RSO interfaces
    @Inject
    @Channel("internal")
    Emitter<String> emitter;


    List<String> incomingValues = new ArrayList<>();

    void addValue(String value) {
        emitter.send(value);
    }

    @Incoming("internal")
    public void sink(String value) {
        synchronized (incomingValues) {
            incomingValues.add(value);
        }
    }

    String getFormattedValues() {
        StringBuilder sb = new StringBuilder();
        synchronized (incomingValues) {
            for (String value : incomingValues) {
                sb.append(value);
                sb.append(";");
            }
        }
        return sb.toString();
    }

}
