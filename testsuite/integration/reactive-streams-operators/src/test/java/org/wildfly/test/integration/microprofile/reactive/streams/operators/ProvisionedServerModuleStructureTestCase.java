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

package org.wildfly.test.integration.microprofile.reactive.streams.operators;

import org.junit.Test;
import org.wildfly.test.integration.microprofile.shared.ModulesChecker;

/**
 * Tests that the provisioned server contains the expected modules from the provisioned layer(s),
 * and that other reactive modules from non-provisioned layers don't exist
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ProvisionedServerModuleStructureTestCase {
    @Test
    public void checkModuleStructure() {
        ModulesChecker checker = ModulesChecker.builder()
                .addExpected("io/reactivex/rxjava2/rxjava/main")
                .addExpected("io/smallrye/reactive/streams-operators/main")
                .addExpected("org/eclipse/microprofile/reactive-streams-operators/api/main")
                .addExpected("org/eclipse/microprofile/reactive-streams-operators/core/main")
                .addExpected("org/reactivestreams/main")
                .addExpected("org/wildfly/security/manager/main")
                .addNotExpected("io/smallrye/context-propagation")
                .addNotExpected("io/smallrye/reactive/messaging")
                .addNotExpected("io/vertx")
                // These are in the reactive-streams-operators-rxjava2 layer which is not provisioned here
                .addNotExpected("io/smallrye/reactive/converters/rxjava2/main")
                .addNotExpected("org/jboss/resteasy/resteasy-rxjava2/main")
                .addNotExpected("io/smallrye/context-propagation/propagators/rxjava2/main")
                .build();

        checker.checkModules();
    }
}
