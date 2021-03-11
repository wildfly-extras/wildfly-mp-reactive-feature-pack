# WildFly Microprofile Reactive Messaging Quickstarts

# Motivation
MicroProfile Reactive Messaging is a framework for building event-driven, data streaming and event-sourcing applications
using CDI. It lets your application interact with various messaging technologies such as Apache Kafka, AMQP, MQTT etc.

MicroProfile Config is used to do the mapping of the messaging providers, so in a lot of cases you can leave the code
unchanged.
 
## Prerequisites 
You will need to have the following installed on your machine:

* JDK 1.8+
* A Docker environment
* Galleon downloaded and available on your path as described in the [main README](../../README.md). The main README also provides
background on how to provision WildFly servers and which Galleon layers are available. 

## Structure
This consists of the following modules
* [core/](core) - This contains the common code for the application, and will be described in more detail below.
* 'Specialisations' - Each of these uses the code from the `core/` module, and provides a `docker-compose.yml` to install 
the target messaging system. They also provide a `provision.xml` to provision a WildFly server with the relevant 
Galleon layers installed. Finally they contain a 
`src/main/resources/META-INF/microprofile-config.properties` which configures the application for use with the target
messaging system while reusing the code. These are in the following sub-modules
    * [amqp/](amqp) - Uses AMQP as the messaging system
    * [mqtt/](mqtt) - Uses MQTT as the messaging system. Due to some limitations in the MQTT protocol, this example
    copies the code from `core/` and adjusts it slightly.

The reason we have the `core` module, is that we use this both for community users to try out things we don't want in
WildFly it self yet, as well as an incubator for things that make it into WildFly itself.
This started off containing a QuickStart for Reactive Messaging with Kafka, which can now be found
[here](https://github.com/wildfly/quickstart/tree/master/microprofile-reactive-messaging-kafka) (Make sure to select
the tag for the WildFly version you are using!). In the future we may add other Reactive Messaging connectors to other
messaging systems.

## How to run it
First you need to build the contents of this repository. You can skip this step if you have checked out a tag since 
then the artifacts are deployed to Maven:
```
mvn install -DskipTests
```
Then using a terminal you go into the folder of the relevant child module (e.g. `kafka/`), and start the messaging 
system by running:
```
docker-compose up
```
Next you need to provision a server (remember you need to have Galleon installed as described in 
the [main README](../../README.md). You can do this by going to the relevant child module (e.g. `kafka/`) in a new
terminal, and then run:
```
galleon.sh provision ./provision.xml --dir=target/my-wildfly
./target/my-wildfly/bin/standalone.sh -Djboss.as.reactive.messaging.experimental=true
```
This provisions the server with the relevant Galleon layers, and starts it. The
[main README](../../README.md) contains information about the layers in this feature pack.

The `-Djboss.as.reactive.messaging.experimental` system property is needed to allow annotations
understood by the SmallRye implementation of MicroProfile Reactive Messaging that are
not part of the MicroProfile Reactive Messaging 1.0 specification such as `@Channel`.

Then in another terminal window, go to the relevant child module directory and run:
```
mvn package wildfly:deploy
```
This builds and deploys the application into the provisioned WildFly server.

Finally go to http://localhost:8080/quickstart/ and see the prices be updated from the application.

## Code structure

### Price Generator
The [PriceGenerator](core/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceGenerator.java) 
is a class containing a method that simulates calling an external resource which returns a random price every 5 seconds:
```
@ApplicationScoped
public class PriceGenerator {
    @Inject
    private MockExternalAsyncResource mockExternalAsyncResource;

    @Outgoing("generated-price")
    public CompletionStage<Integer> generate() {
        return mockExternalAsyncResource.getNextValue();
    }
}
```
It is an `ApplicationScoped` CDI bean. The important part is the `@Outgoing` annotation, which pushes the values
from the `Flowable` reactive stream returned by the method to the `generated-price` stream. Later we will see how
we use a `META-INF/microprofile-config.properties` to bind this stream to the underlying messaging provider.

### Price Converter
Next we have the [PriceConverter](core/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceConverter.java)
which reads values off a stream and transforms them:
```
@ApplicationScoped
public class PriceConverter {
    private static final double CONVERSION_RATE = 0.88;

    @Incoming("prices")
    @Outgoing("my-data-stream")
    @Broadcast
    public double process(int priceInUsd) {
        return priceInUsd * CONVERSION_RATE;
    }
```
This method consumes values off the `prices` topic/stream, does some conversion, and the value returned from the method
is sent to the `my-data-stream` stream. The `@BroadCast` annotation is a SmallRye (the implementation we are using
in this feature pack to support Reactive Messaging) extension to the Reactive Messaging specification.
The `Broadcast` annotation allows more than one subscriber to a stream (normally there must be a one to one mapping) and
all subscribers will receive the value.
**Note:** `my-data-stream` is an in-memory stream which is not connected to a messaging provider. We will consume this
stream in the `PriceResource` in the next step.

**Note:** The MQTT example is exactly the same but lives in the [mqtt](mqtt/src/main/) source folder. The only
difference is the signature of the [PriceConverter.process()](mqtt/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceConverter.java#L36)
method. MQTT can only handle byte arrays for `@Incoming` methods. For the other quickstarts
we use an `int` for this method as shown above.

### Price Resource
Finally we have a JAX-RS resource implemented in [PriceResource](core/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceResource.java)
```
@Path("/prices")
public class PriceResource {
    @Inject
    @Channel("my-data-stream") Publisher<Double> prices;


    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS) // denotes that server side events (SSE) will be produced
    @SseElementType("text/plain") // denotes that the contained data, within this SSE, is just regular text/plain data
    public Publisher<Double> stream() {
        return prices;
    }
}
```
This does a few things:
* The `@Path` annotation binds the JAX-RS resource to the `/prices` path.
* We inject the `my-data-stream` channel using the `@Channel` qualifier. This allows us to take data from reactive 
messaging and push them to the 'imperative' part of our application. `@Channel` is a SmallRye extension to the
Reactive Messaging specification. It has however been merged to the upstream branch of the specification, so
it is expected to be part of the next version (1.1) of the specification.
* The `stream()` method produces server side events of type `text/plain`, and returns the `prices` stream that was 
injected in the previous step.

### The HTML page
The [index.html](core/src/main/webapp/index.html) page consumes the server sent events and displays those:
```
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Prices</title>

    <link rel="stylesheet" type="text/css"
          href="https://cdnjs.cloudflare.com/ajax/libs/patternfly/3.24.0/css/patternfly.min.css">
    <link rel="stylesheet" type="text/css"
          href="https://cdnjs.cloudflare.com/ajax/libs/patternfly/3.24.0/css/patternfly-additions.min.css">
</head>
<body>
<div class="container">

    <h2>Last price</h2>
    <div class="row">
    <p class="col-md-12">The last price is <strong><span id="content">N/A</span>&nbsp;&euro;</strong>.</p>
    </div>
</div>
</body>
<script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
<script>
    var source = new EventSource("/prices/stream");
    source.onmessage = function (event) {
        document.getElementById("content").innerHTML = event.data;
    };
</script>
</html>
```
As prices are published in the JAX-RS resource, they are displayed on the page.

### Mapping the streams
See each of the child modules for how we map our application's streams to the underlying messaging provider:
* [amqp/](amqp/)
* [kafka/](kafka/)


## Further reading
See the [References](../../README.md#references) section of the main README for links to the specs, and the 
[SmallRye](https://smallrye.io) implementations and documentation.