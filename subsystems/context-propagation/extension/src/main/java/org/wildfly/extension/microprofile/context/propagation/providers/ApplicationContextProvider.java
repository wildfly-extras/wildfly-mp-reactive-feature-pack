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

package org.wildfly.extension.microprofile.context.propagation.providers;

import java.security.PrivilegedAction;
import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ApplicationContextProvider implements ThreadContextProvider {
    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return ApplicationThreadContextSnapshot.create(true);
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return ApplicationThreadContextSnapshot.create(false);
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.APPLICATION;
    }

    private static class ApplicationThreadContextSnapshot implements ThreadContextSnapshot {
        final ClassLoader tccl;
        final boolean propagate;

        private ApplicationThreadContextSnapshot(ClassLoader tccl, boolean propagate) {
            this.tccl = tccl;
            this.propagate = propagate;
        }

        static ApplicationThreadContextSnapshot create(boolean propagate) {
            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            return new ApplicationThreadContextSnapshot(tccl, propagate);
        }

        @Override
        public ThreadContextController begin() {
            if (propagate) {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
            } else {
                ClassLoader system = WildFlySecurityManager.doChecked(GetSystemClassLoaderAction.INSTANCE);
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(system);
            }
            return new ThreadContextController() {
                @Override
                public void endContext() throws IllegalStateException {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
                }
            };
        }
    }

    private static class GetSystemClassLoaderAction implements PrivilegedAction<ClassLoader> {
        static final GetSystemClassLoaderAction INSTANCE = new GetSystemClassLoaderAction();
        @Override
        public ClassLoader run() {
            return ClassLoader.getSystemClassLoader();
        }
    }

}
