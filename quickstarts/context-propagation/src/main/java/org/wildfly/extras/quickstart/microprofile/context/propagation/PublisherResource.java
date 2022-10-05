package org.wildfly.extras.quickstart.microprofile.context.propagation;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;



/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/")
@ApplicationScoped
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