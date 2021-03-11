package org.wildfly.extension.microprofile.context.propagation.mutiny;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.mutiny.context.BaseContextPropagationInterceptor;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WildFlyContextPropagationDecorator extends BaseContextPropagationInterceptor {
    @Override
    protected SmallRyeThreadContext getThreadContext() {
        ThreadContext tc = ThreadContextRegistry.INSTANCE.getThreadContext();
        return (SmallRyeThreadContext) tc;
    }

    @Override
    public int ordinal() {
        return 0;
    }
}
