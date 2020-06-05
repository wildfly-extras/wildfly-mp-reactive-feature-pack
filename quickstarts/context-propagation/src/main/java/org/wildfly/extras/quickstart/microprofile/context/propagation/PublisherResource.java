package org.wildfly.extras.quickstart.microprofile.context.propagation;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;



/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/")
@RequestScoped
public class PublisherResource {
    @PersistenceContext(unitName = "quickstart")
    EntityManager em;

    // Get the prices stream
    @Inject
    @Channel("prices") Publisher<Double> prices;


    @Transactional // This method is transactional
    @GET
    @Path("/prices")
    @Produces(MediaType.SERVER_SENT_EVENTS) // denotes that server side events (SSE) will be produced
    @SseElementType(MediaType.TEXT_PLAIN) // denotes that the contained data, within this SSE, is just regular text/plain data
    public Publisher<Double> prices() {
        // get the next three prices from the price stream
        return ReactiveStreams.fromPublisher(prices)
                .limit(3)
                .map(price -> {
                    // Context propagation makes this block inherit the transaction of the caller
                    System.out.println("Storing price: " + price);
                    // store each price before we send them
                    Price priceEntity = new Price();
                    priceEntity.setValue(price);
                    // here we are all in the same transaction
                    // thanks to context propagation
                    em.persist(priceEntity);

                    return price;
                })
                .buildRs();
    }

    @GET
    @Path("/prices/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Price> getAllPrices() {
        TypedQuery<Price> query = em.createQuery("SELECT p from Price p", Price.class);
        return query.getResultList();
    }
}