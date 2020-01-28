# Reactive Messaging Kafka Connector Quickstart

See the [parent README](..) for a description of the application and how to run it. 

This module is only concerned with:
* Reusing the code from the [../core](../core) module which contains the application java code
* Providing a `docker-compose.yml` file to start up a Kafka server
* Providing a `provision.xml` file to make it simple to use Galleon to provision a WildFly server that contains the
needed layers
* Providing a `META-INF/microprofile-config.properties` file to map the streams used by the core application to the Kafka
server

## Configuration
The [src/main/resources/META-INF/microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties) 
file contains the configuration to map the streams used by the application to Kafka streams. The application has two 
streams that are backed by Kafka: `generated-prices` and `prices`.

```
# Configure the Kafka sink (we write to it)
mp.messaging.outgoing.generated-price.connector=smallrye-kafka
mp.messaging.outgoing.generated-price.topic=prices
mp.messaging.outgoing.generated-price.value.serializer=org.apache.kafka.common.serialization.IntegerSerializer

# Configure the Kafka source (we read from it)
mp.messaging.incoming.prices.connector=smallrye-kafka
mp.messaging.incoming.prices.topic=prices
mp.messaging.incoming.prices.value.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer
```

The format of the keys are
```
mp.messaging.[outgoing|incoming].{channel-name}.property=value
```
The `channel-name` segment must match the value set in the `@Incoming` and `@Outgoing` annotations on the methods.

The `connector` segment has a value of `smallrye-kafka` which makes SmallRye use the Kafka connector.

The `prices` segment specifies that we should read from/write to a stream called `prices`.

Since we are using Integers in our example, we use the Kafka `IntegerDeserializer`. The 
Kafka [Producer](https://kafka.apache.org/documentation/#producerconfigs) and 
[Consumer](https://kafka.apache.org/documentation/#consumerconfigs) documentation contains more information about more 
values you can use to configure how you interact with Kafka.