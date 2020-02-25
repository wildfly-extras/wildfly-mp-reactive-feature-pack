![CI](https://github.com/kabir/wildfly-mp-reactive-galleon-pack/workflows/Galleon%20Pack%20Template%20Java%20CI/badge.svg)

# WildFly MicroProfile Reactive Feature Pack

This repository contains a Galleon feature pack to add the MicroProfile reactive subsystems to WildFly.
Galleon is the tool we use internally to build the WildFly server. See the Galleon 
[documentation](https://docs.wildfly.org/galleon/) for more background about the concepts.

To install the feature pack you need to [download Galleon](https://github.com/wildfly/galleon/releases) and unzip
it somewhere. The rest of this document assumes that you have added the resulting `galleon-x.y.z.Final/bin/` folder
to your path. 

The easiest way to install a WildFly Server with MicroProfile reactive specs is 
(Download [provision.xml](provision.xml) first): 
```
galleon.sh provision /path/to/provision.xml --dir=wildfly
```
This installs a full WildFly installation and everything from this feature pack in the `wildfly/` directory.
Later we will look at what the above command means, other ways to install the server and how to tweak what is
installed. 

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

-----------
## WildFly Layers
There is a list of all our layers defined by WildFly and WildFly Core in our 
[documentation](https://docs.wildfly.org/18/Admin_Guide.html#wildfly-galleon-layers).

However, if you want to understand better what their dependencies are, you can look at the 
layer-spec.xml for the various layers in the following locations:
* WildFly Core's [Core Feature Pack](https://github.com/wildfly/wildfly-core/tree/11.0.0.Final/core-galleon-pack/src/main/resources/layers/standalone)
* WildFly's [Servlet Feature Pack](https://github.com/wildfly/wildfly/tree/19.0.0.Final/servlet-galleon-pack/src/main/resources/layers/standalone)
* WildFly's [Full Feature Pack](https://github.com/wildfly/wildfly/tree/19.0.0.Final/galleon-pack/src/main/resources/layers/standalone)

Note that the above links take you to the versions used for WildFly 19.0.0.Final. If you
are interested in another/newer WildFly version, adjust the tag name in the URL. 

-------------
## Layers in this Feature Pack 

The layers from this feature pack are contained in the 
[galleon-pack/src/main/resources/layers/standalone](galleon-pack/src/main/resources/layers/standalone)
folder. We'll explain what each of them contains, and their direct dependencies on other layers.

### context-propagation
The `context-propagation` layer installs the `microprofile-context-propagation-smallrye` subsystem, so you can use
the MicroProfile [Context Propagation](https://github.com/eclipse/microprofile-context-propagation) APIs 
from your application.

**Note:** although the core context propagation mechanism works, we are still missing things in WildFly 19 for this
to work properly. Currently the only things which are propagated properly are `cdi` and `application` (which essentially
means the Thread Context ClassLoader)

Layer Dependencies:
* `cdi` - From WildFly's Full Feature Pack. It contains the `weld` subsystem which implements Jakarta EE CDI.
* `microprofile-config` - From WildFly's Full Feature Pack. It contains the `microprofile-config-smallrye` subsystem
which implements MicroProfile Config. 
* `transactions` - From WildFly's Full Feature Pack. It contains the `transactions` subsystem which contains the 
`TransactionManager`. This is needed for propagation of the current transaction.

### reactive-streams-operators
The `reactive-streams-operators` layer installs the `microprofile-reactive-streams-operators-smallrye` subsystem,
so you can use the MicroProfile [Reactive Streams Operators](https://github.com/eclipse/microprofile-reactive-streams-operators)
classes from your application. 

Layer Dependencies:
* `cdi` - From WildFly's Full Feature Pack. It contains the `weld` subsystem which implements Jakarta EE CDI.

### reactive-messaging
The `reactive-messaging` layer installs the `microprofile-reactive-messaging-smallrye` subsystem,
which implements MicroProfile [Reactive Messaging](https://github.com/eclipse/microprofile-reactive-messaging) 
functionality. 

**Note:** this layer only installs the core functionality which gives you non-blocking asynchronous
message passing within your application. To install connectors to interact with external systems you need to 
install one of the layers that provide connectors to these external systems. They are listed in the below sub-sections.

Layer Dependencies:
* `cdi` - From WildFly's Full Feature Pack. It contains the `weld` subsystem which implements Jakarta EE CDI.
* `microprofile-config` - From WildFly's Full Feature Pack. It contains the `microprofile-config-smallrye` subsystem
which implements MicroProfile Config. 
* `reactive-streams-operations` - From this feature pack, as described above. 
* `transactions` - From WildFly's Full Feature Pack. It contains the `transactions` subsystem which contains the 
`TransactionManager`.

#### reactive-messaging-amqp
The `reactive-messaging-amqp` layer installs the AMQP connector so you can interact with AMQP enabled message
brokers.

Layer Dependencies:
* `reactive-messaging` - From this feature pack, as described above. 

#### reactive-messaging-kafka
The `reactive-messaging-amqp` layer installs the Kafka connector so you can interact with Kafka streams.

Layer Dependencies:
* `reactive-messaging` - From this feature pack, as described above. 

#### reactive-messaging-mqtt
The `reactive-messaging-mqtt` layer installs the MQTT connector so you can interact with a MQTT server.

Layer Dependencies:
* `reactive-messaging` - From this feature pack, as described above. 

#### reactive-messaging-connectors

The `reactive-messaging-connectors` layer is a convenience layer, which installs all the connector layers mentioned above.

Layer Dependencies:
* `reactive-messaging-amqp` - From this feature pack, as described above. 
* `reactive-messaging-kafka` - From this feature pack, as described above. 
* `reactive-messaging-mqtt` - From this feature pack, as described above. 

### microprofile-reactive-all
The `microprofile-reactive-all` layer is a convenience layer, which installs all the layers mentioned above.

---------------------

## Installing the MicroProfile Reactive Layers
Download Galleon as mentioned in the introduction. There are two main ways of provisioning a server containing the 
MicroProfile. The first is to provision from a file, as we saw in the introduction. The second is to 
execute all the Galleon commands individually.

In both cases we install the main WildFly server (possibly with some tweaks) and then we install layers
we choose from this feature pack.

Galleon can not modify the server you download from the [wildfly.org downloads page](https://wildfly.org/downloads/). 
Instead you have to install WildFly using Galleon before adding the layers from this feature pack. Both the ways do 
this.

### Provision from a File
The [provision.xml](provision.xml) file contains everything we need to install a server with the MicroProfile reactive
spec subsystems.

It contains a reference to the WildFly feature pack. It's version is `current` which means it will download the 
latest released version (which at the time of writing is 19.0.0.Final. If you want to choose a different version, 
you can modify the file and append the version as `current:19.0.0.Beta2` for example. Note that WildFly 19.0.0.Final
is the first version this feature pack can be installed into.

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
at the time of writing is 19.0.0.Final. If you want to install a particular version of 
WildFly, you can append the version, e.g:

* `wildfly:current#18.0.0.Final` - installs WildFly 18.0.0.Final
* `wildfly:current#19.0.0.Beta2-SNAPSHOT` - installs WildFly from locally build maven artifacts

Note that the minimal supported WildFly version for this feature pack is 19.0.0.Final. 

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
```
galleon.sh install org.wildfly.extras.galleon-feature-pack-template:template-galleon-pack:1.0.0.Alpha-SNAPSHOT --layers=microprofile-reactive-all --dir=wildfly
``` 
which will install all the layers from the MicroProfile reactive feature pack.

To just install the `reactive-streams-operators` and `context-propagation` layers, we run this instead:
```
galleon.sh install org.wildfly.extras.galleon-feature-pack-template:template-galleon-pack:1.0.0.Alpha-SNAPSHOT --layers=reactive-streams-operators,context-propagation --dir=wildfly
``` 
----
## Quickstarts
Now that you know how to install a WildFly server with the MicroProfile reactive feature pack subsystems,
take a look at our [Quickstarts](quickstarts/) for examples of how to use them.
