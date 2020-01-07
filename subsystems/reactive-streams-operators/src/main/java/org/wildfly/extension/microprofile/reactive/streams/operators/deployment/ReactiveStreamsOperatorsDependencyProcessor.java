/*
 * Copyright 2019 Red Hat, Inc.
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

package org.wildfly.extension.microprofile.reactive.streams.operators.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveStreamsOperatorsDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        addModuleDependencies(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addModuleDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.reactive-streams-operators.core", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.reactive-streams-operators.api", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.reactivestreams", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.reactive.streams-operators", false, false, true, false));

        // Converters
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.reactive.converters.api", false, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.reactive.converters.rxjava2", false, false, true, false));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.jboss.resteasy.resteasy-rxjava2", false, false, true, false));
    }
}
