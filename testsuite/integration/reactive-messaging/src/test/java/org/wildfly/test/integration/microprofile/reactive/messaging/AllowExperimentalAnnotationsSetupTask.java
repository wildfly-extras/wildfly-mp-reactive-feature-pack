package org.wildfly.test.integration.microprofile.reactive.messaging;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AllowExperimentalAnnotationsSetupTask extends CLIServerSetupTask {
    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        this.builder.node(containerId)
                .setup("/system-property=jboss.as.reactive.messaging.experimental:add(value=true)")
                .teardown("/system-property=amqp-port:remove");
        super.setup(managementClient, containerId);
    }
}
