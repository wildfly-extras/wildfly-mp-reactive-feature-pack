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

package org.wildfly.test.integration.microprofile.reactive.messaging;

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
                .addExpected("io/smallrye/reactive/messaging/main")
                .addExpected("io/smallrye/reactive/messaging/connector/main")
                .addExpected("io/vertx/client/main")
                .addExpected("io/smallrye/reactive/streams-operators")
                .addNotExpected("io/smallrye/reactive/messaging/connector/kafka")
                .addNotExpected("io/smallrye/reactive/messaging/connector/amqp")
                .addNotExpected("io/smallrye/reactive/messaging/connector/mqtt")
                .addNotExpected("io/smallrye/context-propagation")
                .addNotExpected("io/vertx/main")
                .addNotExpected("io/vertx/client/amqp")
                .addNotExpected("io/vertx/client/kafka")
                .addNotExpected("io/vertx/client/mqtt")
                .build();

        checker.checkModules();
    }
}
