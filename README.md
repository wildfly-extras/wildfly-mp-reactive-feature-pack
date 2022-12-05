![CI](https://github.com/kabir/wildfly-mp-reactive-feature-pack/workflows/Galleon%20Pack%20Template%20Java%20CI/badge.svg)

> :warning: **This stream of the feature pack only supports WildFly 23. For previous versions of WildFly, try the
[1.0.x releases](https://github.com/wildfly-extras/wildfly-mp-reactive-feature-pack/releases).** This is because
MicroProfile Reactive Messaging and the Kafka integration moved into WildFly 23. Prior to WildFly 23, those pieces
of functionality were supplied by this feature pack.

# WildFly MicroProfile Reactive Feature Pack

**If you are an end-user and not a project developer (or especially interested), use the latest tag for this README and other information.**

This repository contains a Galleon feature pack to extend the MicroProfile reactive subsystems offered by WildFly.
WildFly 23 itself contains:
* MicroProfile Reactive Messaging 1.0
** The Kafka connector
* MicroProfile Reactive Streams Operators 1.0

This feature pack contains functionality to:
* Allow MicroProfile Reactive Messaging functionality beyond what is offered by 1.0, by passing in the
`-Djboss.as.reactive.messaging.experimental=true` system property on server start. This allows you to use `@Channel`
and `Emitter` which were introduced after 1.0. These allow you to have user-initiated code interact with the streams
managed by Reactive Messaging.
* Enable MicroProfile Context Propagation 1.0. This is useful in the context of user-initiated code interacting with
the streams, for example to be able to propagate the main thread's transaction to reactive style callouts which might be
happening in another thread.
* Enable other connectors for Reactive Messaging, currently we have
** MQTT
** AMQP

If you would like to see more Reactive Messaging connectors here and are willing to give integrating them a go, we use the
[SmallRye](https://github.com/smallrye/smallrye-reactive-messaging) implementations. Get in touch if you are interested,
and I can offer advice.

# Installing the Feature Pack
Galleon is the tool we use internally to build the WildFly server. See the Galleon 
[documentation](https://docs.wildfly.org/galleon/) for more background about the concepts.

**If you are an end-user and not a project developer (or especially interested), use the latest tag for this 
README and other information.**  

To install the feature pack you need to [download Galleon](https://github.com/wildfly/galleon/releases) (use the 5.x series)
and unzip it somewhere. The rest of this document assumes that you have added the resulting `galleon-x.y.z.Final/bin/` folder
to your path. 

The easiest way to install a WildFly Server with MicroProfile reactive specs is 
(Download [provision.xml](provision.xml) first): 
```
galleon.sh provision /path/to/provision.xml --dir=wildfly
```
This installs a full WildFly installation and everything from this feature pack in the `wildfly/` directory.
Later we will look at what the above command means, other ways to install the server and how to tweak what is
installed. 

-------------

## Quickstarts
Take a look at our [Quickstarts](quickstarts/) for examples of how to use the MicroProfile Reactive
specifications in your applications.

The rest of this README will explain the structure of this feature pack in more detail. If you 
are itching to get going try the quickstarts first and come back to this.

-----------

## Layers Introduction
To provision a server containing the microprofile reactive specs, you use Galleon to first install the base 
WildFly server, and then add `layers` from this feature pack. We will see exactly how this is done later in this guide,
but it essentially allows you to create smaller servers containing exactly the parts of functionality that
you want for your application.

A layer is a unit of functionality. Each subsystem in WildFly has its own layer. For example there is 
a `cdi`  layer for the weld subsystem, a `jaxrs` layer for the jaxrs subsystem and 
an `undertow` layer for the undertow subsystem.

Layers can have dependencies between them, and when asking to install a particular layer, all the 
layer's dependencies are also installed recursively.  

There are some 'aggregate' layers, provided for convenience, such as `cloud-server` which pulls in a set of layers 
that we deem important for a server running on OpenShift. It results in a server with JAX-RS, JPA, Transactions amd 
Weld (CDI) amongst other things. 

## WildFly Layers
There is a list of all our layers defined by WildFly and WildFly Core in our 
[documentation](https://docs.wildfly.org/23/Admin_Guide.html#wildfly-galleon-layers).

If you want to understand better what their dependencies are, you can look at the
layer-spec.xml for the various layers in the following locations:
* WildFly Core's [Core Feature Pack](https://github.com/wildfly/wildfly-core/tree/15.0.0.Final/core-feature-pack/galleon-common/src/main/resources/layers/standalone)
* WildFly's [Servlet Feature Pack](https://github.com/wildfly/wildfly/tree/23.0.0.Final/servlet-feature-pack/galleon-common/src/main/resources/layers/standalone)
* WildFly's [EE Feature Pack](https://github.com/wildfly/wildfly/tree/23.0.0.Final/ee-feature-pack/galleon-common/src/main/resources/layers/standalone)
* WildFly's [MicroProfile Feature Pack](https://github.com/wildfly/wildfly/tree/23.0.0.Final/microprofile/galleon-common/src/main/resources/layers/standalone)

We will use 'WildFly Galleon Pack' to refer to these various feature packs.

Note that the above links take you to the versions used for WildFly 23.0.0.Final. If you
are interested in another/newer WildFly version, adjust the tag name in the URL. 

-------------
## Layers in this Feature Pack 

The layers from this feature pack are contained in the 
[feature-pack/src/main/resources/layers/standalone](feature-pack/src/main/resources/layers/standalone)
folder. We'll explain what each of them contains, and their direct dependencies on other layers.

### microprofile-context-propagation
The `microprofile-context-propagation` layer installs the `microprofile-context-propagation-smallrye` subsystem, so you can use
the MicroProfile [Context Propagation](https://github.com/eclipse/microprofile-context-propagation) APIs 
from your application. The traditional way of propagating state using ThreadLocals does not work well in the reactive
world. Async/reactive code often creates a 'pipeline' of code blocks that get executed 'later' - in practice after the
method defining them has returned. MicroProfile Context Propagation is there to help you deal with this, so that your
deferred code can still for example latch onto the transaction initiated by the calling method.

Note although the core context propagation mechanism works, we are still missing things in WildFly 20 for this
to work totally. You currently get context propagation for the following:
* `Application` - This propagates the Thread Context ClassLoader
* `CDI` - propagates the CDI context
* `Transaction` - **not** enabled by default, see the `microprofile-context-propagation-jta` layer below

What is missing is:
* `Web` = propagation of parameter injected web context
* `RestEasy` - propagation of parameter injected RestEasy context
* `Security` - propagation of the security context

This might still be enough for your application, and we hope to be able to add these soon. 

Layer Dependencies:
* `cdi` - From the WildFly Galleon Pack. It contains the `weld` subsystem which implements Jakarta EE CDI.
* `microprofile-config` - From the WildFly Galleon Pack. It contains the `microprofile-config-smallrye` subsystem
which implements MicroProfile Config. 
* `microprofile-reactive-streams-operators` - From the WildFly Galleon Pack. It contains the
`microprofile-reactive-streams-operators-smallrye` subsystem so you can use the MicroProfile
[Reactive Streams Operators](https://github.com/eclipse/microprofile-reactive-streams-operators)
classes from your application.

#### microprofile-context-propagation-jta
The `microprofile-context-propagation-jta` layer installs the ThreadContextProvider propagating transactions.

Layer Dependencies:
* `microprofile-context-propagation` - From this feature pack, as described in this document.
* `transactions` - From the WildFly Galleon Pack. It contains the `transactions` subsystem which contains the
`TransactionManager`. This is needed for propagation of the current transaction.

### MicroProfile Reactive Messaging Connectors
#### microprofile-reactive-messaging-amqp
The `microprofile-reactive-messaging-amqp` layer installs the AMQP connector so you can interact with AMQP enabled message
brokers.

Layer Dependencies:
* `microprofile-reactive-messaging` - From this feature pack, as described above.

#### microprofile-reactive-messaging-kafka
This is no longer part of this feature pack, as it has been moved to the WildFly Galleon Pack. But you can
still use it when provisioning a server with other parts of this feature pack.
The `microprofile-reactive-messaging-kafka` layer installs the Kafka connector so you can interact with Kafka streams.

Layer Dependencies:
* `microprofile-reactive-messaging` - From the WildFly Galleon Pack. It contains the
`microprofile-reactive-messaging-smallrye` subsystem so you can use the MicroProfile
[Reactive Messaging](https://github.com/eclipse/microprofile-reactive-messaging)
classes from your application.

#### microprofile-reactive-messaging-mqtt
The `microprofile-reactive-messaging-mqtt` layer installs the MQTT connector so you can interact with a MQTT server.

Layer Dependencies:
* `microprofile-reactive-messaging` - From the WildFly Galleon Pack. It contains the
`microprofile-reactive-messaging-smallrye` subsystem so you can use the MicroProfile
[Reactive Messaging](https://github.com/eclipse/microprofile-reactive-messaging)
classes from your application.

#### microprofile-reactive-messaging-connectors

The `microprofile-reactive-messaging-connectors` layer is a convenience layer, which installs all the connector layers mentioned above.

Layer Dependencies:
* `microprofile-reactive-messaging-amqp` - From this feature pack, as described above.
* `microprofile-reactive-messaging-kafka` - From the WildFly Galleon Pack. It installs the Kafka connector so you
can interact with a Kafka server.
* `microprofile-reactive-messaging-mqtt` - From this feature pack, as described above.

### microprofile-reactive-all
The `microprofile-reactive-all` layer is a convenience layer, which installs all the layers mentioned above.

---------------------

## Installing the MicroProfile Reactive Layers
Download Galleon as mentioned in the introduction. There are two main ways of provisioning a server containing the 
MicroProfile Reactive specifications. The first is to provision from a file, as we saw in the introduction. The second is to 
execute all the Galleon commands individually.

In both cases we install the main WildFly server (possibly with some tweaks) and then we install layers
we choose from this feature pack.

Galleon can not modify the server you download from the [wildfly.org downloads page](https://wildfly.org/downloads/). 
Instead you have to install WildFly using Galleon before adding the layers from this feature pack. Both the ways shown do 
this.

### Provision from a File
The [provision.xml](provision.xml) file contains everything we need to install a server with the MicroProfile reactive
spec subsystems.

It contains a reference to the WildFly feature pack. It's version is `current` which means it will download the 
latest released version (which at the time of writing is 23.0.0.Final. If you want to choose a different version,
you can modify the file and append the version as `current:23.0.1.Final` for example.

Next it contains a reference to this feature pack.

Finally it lists the layers to install:
* `cloud-profile` is from the WildFly Full Feature Pack, and is similar to `cloud-server` but smaller.
* `microprofile-reactive-all` installs everything from this feature pack  
  
To adjust what exactly you want to install, you can modify this file. As we saw in the introduction we can
run:
```
galleon.sh provision /path/to/provision.xml --dir=wildfly
```
This installs a full WildFly installation and everything from this feature pack in the `wildfly/` child directory
indicated by the `-dir` flag. 


### Using Galleon Commands
We need to first install our base WildFly server, and then add the layers from this feature pack. This is great
if you want to experiment with different combinations. Once you are happy you can create a provision.xml
file like we saw above.

#### Install the Base Server
First we need to install base server. To do this, we run Galleon to install the full WildFly server (the 
result will be the same as the zip you download from the [wildfly.org downloads page](https://wildfly.org/downloads/)):
```
galleon.sh install wildfly:current --dir=wildfly
```
The `wildfly:current` above tells Galleon to provision the latest version of WildFly which
at the time of writing is 23.0.0.Final. If you want to install a particular version of
WildFly, you can append the version, e.g:

<!-- Leave this as is -->
* `wildfly:current#23.0.0.Final` - installs WildFly from locally build maven artifacts

Note that the minimal supported WildFly version for this feature pack is 23.0.0.Final.

`--dir` specifies the directory to install the server into. In this case I am using 
a relative directory called `wildfly`. 

The first time you do this, it will probably take some time while it downloads everything. After the
first installation it should be a lot faster.

If you want to trim the base server that we install you can specify which layers to install by passing in 
the `--layers` option. For example to install a smaller base server, you can run the following instead:
```
galleon.sh install wildfly:current --dir=wildfly --layers=cloud-profile
```
Note that we do not install our MicroProfile reactive layers yet, because they are unknown in the main
WildFly feature pack. We will add those in the next step. 

If you want to keep your server as small as possible, and you miss something that is ok. You
can rerun the above command, and pass in more layers in the `--layers` option if you missed something.
   
#### Install the MicroProfile Reactive Feature Pack
Now to install our feature pack, we can run:
<!-- Should be SNAPSHOT while working and not when we do a release -->
```
galleon.sh install org.wildfly.extras.reactive:wildfly-microprofile-reactive-feature-pack:3.0.1.Final-SNAPSHOT --layers=microprofile-reactive-all --dir=wildfly
```
which will install all the layers from the MicroProfile reactive feature pack.

To just install the `microprofile-reactive-streams-operators` and `microprofile-context-propagation` layers, we run this instead:
<!-- Should be SNAPSHOT while working and not when we do a release -->
```
galleon.sh install org.wildfly.extras.reactive:wildfly-microprofile-reactive-feature-pack:3.0.1.Final-SNAPSHOT --layers=microprofile-reactive-streams-operators,microprofile-context-propagation --dir=wildfly
```
----
## References
* [MicroProfile Reactive Messaging specification](https://github.com/eclipse/microprofile-reactive-messaging/releases)
* [SmallRye Reactive Messaging documentation](https://smallrye.io/smallrye-reactive-messaging/)
* [SmallRye Reactive Messaging Source](https://github.com/smallrye/smallrye-reactive-messaging)  
* [MicroProfile Reactive Streams Operators specification](https://github.com/eclipse/microprofile-reactive-streams-operators/releases)
* [SmallRye Reactive Streams Operators documentation](https://smallrye.io/smallrye-reactive-streams-operators/)
* [SmallRye Reactive Streams Operators source code](https://github.com/smallrye/smallrye-mutiny/tree/master/reactive-streams-operators)
* [MicroProfile Context Propagation specification](https://github.com/eclipse/microprofile-context-propagation/releases)
* [SmallRye Context Propagation source code](https://github.com/smallrye/smallrye-context-propagation)
