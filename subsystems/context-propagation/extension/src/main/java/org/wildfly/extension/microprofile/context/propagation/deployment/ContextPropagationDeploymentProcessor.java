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

package org.wildfly.extension.microprofile.context.propagation.deployment;

import org.eclipse.microprofile.context.spi.ContextManager;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.context.propagation.mutiny.ThreadContextRegistry;
import org.wildfly.security.manager.WildFlySecurityManager;

import io.smallrye.context.SmallRyeContextManagerProvider;

/**
 */
public class ContextPropagationDeploymentProcessor implements DeploymentUnitProcessor {

    private final String weldCapabilityName;

    public ContextPropagationDeploymentProcessor(String weldCapabilityName) {
        this.weldCapabilityName = weldCapabilityName;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        // TODO install extension to do any needed CDI stuff
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        Module module = context.getAttachment(Attachments.MODULE);
        ClassLoader classLoader = module.getClassLoader();

        ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            // Need to do this using the deployment classloader in case the provider is not yet initialised
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            SmallRyeContextManagerProvider contextManagerProvider = SmallRyeContextManagerProvider.instance();

            ContextManager mgr = contextManagerProvider.findContextManager(classLoader);
            if (mgr != null) {
                contextManagerProvider.releaseContextManager(mgr);
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }

        // Clean up cached thread contexts used by the Mutiny interceptors
        ThreadContextRegistry tcr = ThreadContextRegistry.INSTANCE;
        tcr.cleanup(classLoader);
    }
}