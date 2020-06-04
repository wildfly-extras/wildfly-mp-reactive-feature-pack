# Reactive Messaging MQTT Connector Quickstart

See the [parent README](..) for a description of the application and how to run it. 

This module is only concerned with:
* It differs from the other quickstarts in that it copies the code from the [../core](../core) module and 
changes the signature of the [PriceConverter.process()](mqtt/src/main/java/org/wildfly/extras/quickstart/microprofile/reactive/messaging/PriceConverter.java#L36)
to take a byte array. MQTT only works on byte arrays for `@Incoming` methods. 
* Providing a `docker-compose.yml` file to start up a MQTT server
* Providing a `provision.xml` file to make it simple to use Galleon to provision a WildFly server that contains the
needed layers
* Providing a `META-INF/microprofile-config.properties` file to map the streams used by the core application to the AMQP
server

## Configuration
The [src/main/resources/META-INF/microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties) 
file contains the configuration to map the streams used by the application to MQTT topics. The application has two 
streams that are backed by MQTT: `generated-prices` and `prices`.

```
# Configure the MQTT connector to write to the `prices`  address
mp.messaging.outgoing.generated-price.connector=smallrye-mqtt
mp.messaging.outgoing.generated-price.topic=prices
mp.messaging.outgoing.generated-price.host=localhost
mp.messaging.outgoing.generated-price.port=1883
mp.messaging.outgoing.generated-price.auto-generated-client-id=true

# Configure the MQTT connector to read from the `prices` queue
mp.messaging.incoming.prices.connector=smallrye-mqtt
mp.messaging.incoming.prices.topic=prices
mp.messaging.incoming.prices.host=localhost
mp.messaging.incoming.prices.port=1883
mp.messaging.incoming.prices.generated-client-id=true
```

The format of the keys are
```
mp.messaging.[outgoing|incoming].{channel-name}.property=value
```
The `channel-name` segment must match the value set in the `@Incoming` and `@Outgoing` annotations on the methods.

The `connector` segment has a value of `smallrye-mqtt` which makes SmallRye use the MQTT connector.

The `address` segment specifies the name of the topic to use.

The [SmallRye Reactive Messaging MQTT connector](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/2/mqtt/mqtt.html) 
documentation contains more information about the available options.