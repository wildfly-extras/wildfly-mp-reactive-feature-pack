package org.wildfly.extras.quickstart.microprofile.context.propagation;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;


/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RequestScoped
@Path("/")
public class PriceResource {

    @Inject @Channel("prices")
    Emitter<Double> emitter;

    @POST
    public void postAPrice(Price price) {
        emitter.send(price.getValue());
    }

}