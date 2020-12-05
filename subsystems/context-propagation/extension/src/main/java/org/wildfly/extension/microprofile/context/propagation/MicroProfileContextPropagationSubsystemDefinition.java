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

package org.wildfly.extension.microprofile.context.propagation;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.wildfly.extension.microprofile.context.propagation.MicroProfileContextPropagationExtension.CONFIG_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.context.propagation.MicroProfileContextPropagationExtension.WELD_CAPABILITY_NAME;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.context.propagation._private.MicroProfileContextPropagationLogger;
import org.wildfly.extension.microprofile.context.propagation.deployment.ContextPropagationDependencyProcessor;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileContextPropagationSubsystemDefinition extends PersistentResourceDefinition {

    private static final String CONTEXT_PROPAGATION_CAPABILITY_NAME = "org.wildfly.microprofile.context-propagation";

    private static final RuntimeCapability<Void> CONTEXT_PROPAGATION_CAPABILITY = RuntimeCapability.Builder
            .of(CONTEXT_PROPAGATION_CAPABILITY_NAME)
            .addRequirements(CONFIG_CAPABILITY_NAME, WELD_CAPABILITY_NAME)
            .build();

    public MicroProfileContextPropagationSubsystemDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        MicroProfileContextPropagationExtension.SUBSYSTEM_PATH,
                        MicroProfileContextPropagationExtension.getResourceDescriptionResolver(MicroProfileContextPropagationExtension.SUBSYSTEM_NAME))
                .setAddHandler(AddHandler.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler())
                .setCapabilities(CONTEXT_PROPAGATION_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                RuntimePackageDependency.required("io.smallrye.context-propagation.api"),
                RuntimePackageDependency.required("io.smallrye.context-propagation"),
                RuntimePackageDependency.required("io.smallrye.reactive.mutiny.context-propagation"),
                RuntimePackageDependency.required("javax.enterprise.api"),
                RuntimePackageDependency.required("org.wildfly.reactive.dep.jts"),
                RuntimePackageDependency.required("org.wildfly.security.manager"));
    }

    static class AddHandler extends AbstractBoottimeAddStepHandler {

        static AddHandler INSTANCE = new AddHandler();

        private AddHandler() {
            super(Collections.emptyList());
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performBoottime(context, operation, model);

            context.addStep(new AbstractDeploymentChainStep() {
                public void execute(DeploymentProcessorTarget processorTarget) {

                    // TODO Put these into Phase.java
                    final int DEPENDENCIES_MICROPROFILE_CONTEXT_PROPAGATION = 6304;
                    final int POST_MODULE_MICROPROFILE_CONTEXT_PROPAGATION = 14240;

                    processorTarget.addDeploymentProcessor(MicroProfileContextPropagationExtension.SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_CONTEXT_PROPAGATION, new ContextPropagationDependencyProcessor());
                    processorTarget.addDeploymentProcessor(MicroProfileContextPropagationExtension.SUBSYSTEM_NAME, POST_MODULE, POST_MODULE_MICROPROFILE_CONTEXT_PROPAGATION, new ContextPropagationDependencyProcessor());
                }
            }, RUNTIME);

            MicroProfileContextPropagationLogger.LOGGER.activatingSubsystem();
        }
    }
}
