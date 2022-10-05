package org.wildfly.extras.quickstart.microprofile.context.propagation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;


/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
@Path("/")
public class PriceResource {

    @Inject @Channel("prices")
    Emitter<Double> emitter;

    @POST
    public void postAPrice(Price price) {
        emitter.send(price.getValue());
    }

}