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

package org.wildfly.test.integration.microprofile.shared;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModulesChecker {

    private List<String> expected = new ArrayList<>();
    private List<String> notExpected = new ArrayList<>();

    public ModulesChecker(Builder builder) {
        this.expected = new ArrayList<>(builder.expected);
        this.notExpected = new ArrayList<>(builder.notExpected);
    }

    public void checkModules() {
        Path modulesRoot = Paths.get(new File("target/wildfly/modules/system/layers/base").toURI());
        for (String s : expected) {
            Path path = modulesRoot.resolve(s);
            if (!Files.exists(path)) {
                throw new IllegalStateException("Expected module path " + path + " does not exist");
            }
        }

        for (String s : notExpected) {
            Path path = modulesRoot.resolve(s);
            if (Files.exists(path)) {
                throw new IllegalStateException("Not expected module path " + path + " exists, but should not");
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> expected = new ArrayList<>();
        private List<String> notExpected = new ArrayList<>();

        public Builder addExpected(String directory) {
            expected.add(directory);
            return this;
        }

        public Builder addNotExpected(String directory) {
            notExpected.add(directory);
            return this;
        }

        public ModulesChecker build() {
            return new ModulesChecker(this);
        }
    }
}
