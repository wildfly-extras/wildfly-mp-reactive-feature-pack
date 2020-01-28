# Reactive Messaging AMQP Connector Quickstart

See the [parent README](..) for a description of the application and how to run it. 

This module is only concerned with:
* Reusing the code from the [../core](../core) module which contains the application java code
* Providing a `docker-compose.yml` file to start up a Kafka server
* Providing a `provision.xml` file to make it simple to use Galleon to provision a WildFly server that contains the
needed layers
* Providing a `META-INF/microprofile-config.properties` file to map the streams used by the core application to the AMQP
server

## Configuration
The [src/main/resources/META-INF/microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties) 
file contains the configuration to map the streams used by the application to AMQP topics. The application has two 
streams that are backed by AMQP: `generated-prices` and `prices`.

```
# Configure the AMQP connector to write to the `prices` address
mp.messaging.outgoing.generated-price.connector=smallrye-amqp
mp.messaging.outgoing.generated-price.address=prices
mp.messaging.outgoing.generated-price.durable=true

# Configure the AMQP connector to read from the `prices` queue
mp.messaging.incoming.prices.connector=smallrye-amqp
mp.messaging.incoming.prices.durable=true
```

The format of the keys are
```
mp.messaging.[outgoing|incoming].{channel-name}.property=value
```
The `channel-name` segment must match the value set in the `@Incoming` and `@Outgoing` annotations on the methods.

The `connector` segment has a value of `smallrye-amqp` which makes SmallRye use the AMQP connector.

The `address` segment specifies the name of the topic to use.

The [SmallRye Reactive Messaging AMQP connector](https://smallrye.io/smallrye-reactive-messaging/#_interacting_using_amqp) 
documentation contains more information about the available options.