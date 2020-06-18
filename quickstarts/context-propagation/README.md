# Context Propagation

# Motivation
Traditional blocking code uses [ThreadLocal](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ThreadLocal.html)
variables to store contextual objects in order to avoid passing them as parameters everywhere. Many 
parts of WildFly require those contextual objects to operate properly: CDI and Transactions for example.

If you write reactive/async code, you have to cut your work into a pipeline of code blocks that get 
executed "later", and in practice after the method you defined them in have returned. As such, 
try/finally blocks as well as ThreadLocal variables stop working, because your reactive code gets 
executed in another thread, after the caller ran its finally block.

MicroProfile Context Propagation was made to make those Quarkus extensions work properly in reactive/async 
settings. It works by capturing those contextual values that used to be in thread-locals, 
and restoring them when your code is called.

## Prerequisites 
You will need to have the following installed on your machine:

* JDK 1.8+
* A Docker environment
* Galleon downloaded and available on your path as described in the [main README](../../README.md). The main README also provides
background on how to provision WildFly servers and which Galleon layers are available.
* httpie (or curl)

## How to run it
First you need to build the contents of this repository:
```
mvn install -DskipTests
```

Next you need to provision a server (remember you need to have Galleon installed as described in 
the [main README](../../README.md)). Go to the folder containing this README in a new
terminal, and then run:
```
galleon.sh provision ./provision.xml --dir=target/my-wildfly
./target/my-wildfly/bin/standalone.sh
```
This provisions the server with the relevant Galleon layers, and starts it. The
[main README](../../README.md) contains information about the layers in this feature pack.

Then in another terminal window, go to the same folder and run:
```
mvn package wildfly:deploy
```
This builds and deploys the application into the provisioned WildFly server.

In yet another terminal window run
```
http :8080/quickstart/prices --stream
```
This command streams the next 3 prices.

Send the prices from another terminal with:
```
http POST :8080/quickstart value:=34 
http POST :8080/quickstart value:=35 
http POST :8080/quickstart value:=36
```
The first terminal containing the `--stream` parameter should have displayed the prices and returns to the 
prompt. Then run, from any terminal:
```
http :8080/quickstart/prices/all
```
It should display the persisted prices. These prices are only persisted when the streams completes (so after having received 3 prices).

## Code structure
### PriceResource
Bound to the REST `/quickstart` endpoint is the 
[PriceResource](src/main/java/org/wildfly/extras/quickstart/microprofile/context/propagation/PriceResource.java). It takes the posted 
prices and publishes them to a Reactive Messaging channel.

```
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
``` 
We inject the `prices` channel using the `@Channel` qualifier. This allows us to push data from reactive 
messaging from the 'imperative' to the 'reactive' part of our application. As we have
annotated a field of type `Emitter`, it is implied that the channel is outgoing. Note that both
`@Channel` and `Emitter` are SmallRye extensions to the Reactive Messaging specification. It has however 
been merged to the upstream branch of the specification, so it is expected to be part of the next version (1.1) of 
the specification.

 As we receive prices posted, we send them via the emitter so they are handled by Reactive Messaging.

### PublisherResource
Next we have the [PublisherResource](src/main/java/org/wildfly/extras/quickstart/microprofile/context/propagation/PublisherResource.java), 
which listens for prices on the 'prices' channel and stores them 
into a database using Hibernate in the same transaction as the calling method.
```
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
}
```
Again we use the `@Channel` annotation which is a SmallRye extension to the specification expected
to be part of the next version it. Since we are annotating a Publisher, it is expected to handle
incoming messages on the `prices` stream.

Since the name of the channel is the same as the outgoing channel in `PriceResource`, an 
in-memory stream is used so there is no need to set up an external messaging broker like we 
did in the Reactive Messaging [quickstart](../reactive-messaging).


The `http :8080/quickstart/prices --stream` call ends up in the `prices()` method. It is transactional, 
reads the next three items from the channel and stores them in a database (using the same transaction as the 
calling method) before returning them to the client. This interaction happens via Reactive Streams Operators.
Thanks to context propagation support this works out of the box when returning a Publisher created via 
MicroProfile Reactive Streams Operators. 

#### Using CompletionStage
A Publisher, as seen above, is good for dealing with streams of data. If you are dealing with
more finite data, it is better to use  [CompletionStage](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/CompletionStage.html)
from the JDK to do the async return from the method. In this case you can still have Context Propagation but you 
need to take some steps. You can do that by injecting a `ThreadContext` or a `ManagedExecutor` that will propagate
all contexts the server has support for. These come from the MicroProfile Context Propagation specification.

The [NameResource](src/main/java/org/wildfly/extras/quickstart/microprofile/context/propagation/NameResource.java)
class demonstrates these as we will see in the next examples.

Whether you use `ThreadContext` or `ManagedExecutor` depends on your use-case.

###### ThreadContext
When you run `http :8080/quickstart/names-tc` you will end up in a method which simulates a slow operation
to generate 3 names. The relevant parts of the code are shown below.
```
    @Inject
    ThreadContext threadContext;

    @Transactional
    @GET
    @Path("/names-tc")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<List<Name>> readAndStoreNamesTc() {
        return threadContext.withContextCapture(generateNamesCompletionStage(3))
                .thenApplyAsync(
                        list -> {
                            for (Name name : list) {
                                // Context propagation makes this block inherit the transaction of the caller
                                System.out.println("Storing name: " + name);
                                // here we are all in the same transaction
                                // thanks to context propagation
                                em.persist(name);
                            }
                            return list;
                        },
                        Executors.newCachedThreadPool());
    }

    CompletionStage<List<Name>> generateNamesCompletionStage(int number) {
       ...
```
As before it stores every name in the database, propagating the calling transaction to the asynchronous code. It 
then returns the list of names to the caller.

###### ManagedExecutor
When you run `http :8080/quickstart/names-me` you will also end up in a method which simulates a slow operation
to generate 3 names. The relevant parts of the code are shown below.
```
    @Inject
    ManagedExecutor executor;

    @Transactional
    @GET
    @Path("/names-me")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<List<Name>> readAndStoreNamesMe() {
        CompletionStage<List<Name>> future = executor.completedStage(new ArrayList<>());
        future.thenApply(list -> {
            for (Name name : generateNames(3)) {
                // Context propagation makes this block inherit the transaction of the caller
                System.out.println("Storing name: " + name);
                // here we are all in the same transaction
                // thanks to context propagation
                em.persist(name);
                list.add(name);
            }
            return list;
        });
        return future;
    }

    private List<Name> generateNames(int number) {
        ...
```
As before it stores every name in the database, propagating the calling transaction to the asynchronous code. It 
then returns the list of names to the caller. 

For both the examples to check that the names were stored, you can run `http :8080/quickstart/names/all`.

## Further reading
See the [References](../../README.md#references) section of the main README for links to the specs, and the 
[SmallRye](https://smallrye.io) implementations and documentation.
