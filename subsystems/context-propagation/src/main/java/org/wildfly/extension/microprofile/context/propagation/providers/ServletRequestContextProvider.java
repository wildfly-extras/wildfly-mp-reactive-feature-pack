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

package org.wildfly.extension.microprofile.context.propagation.providers;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.undertow.servlet.handlers.ServletRequestContext;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServletRequestContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        ServletRequestContext captured = ServletRequestContext.current();
        return () -> {
            ServletRequestContext current = restore(captured);
            return () -> restore(current);
        };
    }

    private ServletRequestContext restore(ServletRequestContext context) {
        ServletRequestContext currentContext = ServletRequestContext.current();
        if (context == null)
            ServletRequestContext.clearCurrentServletAttachments();
        else
            ServletRequestContext.setCurrentRequestContext(context);
        return currentContext;
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            ServletRequestContext current = restore(null);
            return () -> restore(current);
        };
    }

    @Override
    public String getThreadContextType() {
        return "Servlet";
    }
}
