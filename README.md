# LWM2M over MQTT
### Introduction
> OMA Lightweight M2M is a protocol for device and service management. The main purpose of this 
> technology is to address service and management needs for constrained M2M devices, over a number
> of transports and bearers. The current stack for LWM2M relies on CoAP as the protocol.

Our solution involves development of an LWM2M server prototype, as well as, a client prototype, which make use of MQTT as the underlying M2M protocol. Thus LWM2M can be used for both CoAP, as well as, MQTT.

In this project we are using
  - Leshan code 
  - Eclipse Paho client library to interact with MQTT broker
  - An existing MQTT broker

---
### Deliverables
In the first phase, we are providing the following LwM2M operations, in plain-text format
  - Registration interface
    -   register
    -   update
    -   de-register
  - Device Management & Service Enablement Interface
    - read
    - write
    - write-attributes
    - execute
    - create
    - delete
    - deregister
  - Information Reporting Interface 
    - observe
    - notify
    - cancel-observation

---
### Version
0.0.1

---
### Technology

LWM2M over MQTT makes use of a number of open source projects to work properly:

* [Eclipse] 
* [Paho Client] - Open source client implementation for M2M / IoT
* [Leshan] - OMA LwM2M implementation in java
* [Open Source Mosquitto Broker] - This MQTT broker is hosted at eclipse 
* [git repository] - Githubs repository where the artifacts are stored

---

## Running the samples

#### Building the project

To build this project, you must have [ant] installed. In the command console, run the following
```shell
$ ant clean
$ ant build
```

This will build the project and generate the jars for
* LeshanStandalone.jar - The lwm2m server over MQTT
* LwM2MAdvClient.jar - The lwm2m client over MQTT for Raspberry Pi
* LwM2MExampleClient.jar - The lwm2m client over MQTT for desktop machines
* mqtt.properties - Properties file containing the connection details

Edit the mqtt.properties to update mqtt broker, serverId, clientId and other parameters.

```javascript
MQTT_SERVER = localhost
MQTT_PORT = 1883
ORGID = eclipse
CLIENT_ID = 10
SERVER_ID = 56783
CLIENT_APPLICATIONID = mqtt-client
SERVER_APPLICATIONID = leshan-server
```

#### Run lwm2m server

Run the following command in command console to start the lwm2m server. `mqtt.properties` must be present in the same directory where you will run this program.
```shell
$ cd jars
//in windows
$ java -classpath ".;LeshanStandalone.jar;..\leshan.jar;..\org.eclipse.paho.client.mqttv3-1.0.0.jar" leshan.standalone.LeshanStandalone
//in Linux
$ java -classpath ".:LeshanStandalone.jar:..\leshan.jar:..\org.eclipse.paho.client.mqttv3-1.0.0.jar" leshan.standalone.LeshanStandalone
```

In your browser, open [http://localhost:8080] to view the lwm2m server dashboard

#### Run lwm2m Client

Run the following command in command console to start the lwm2m Client. `mqtt.properties` must be present in the same directory where you will run this program.
```shell
$ cd jars
//In raspberry Pi
$  java -cp .:LwM2MAdvClient.jar:org.eclipse.paho.client.mqttv3-1.0.0.jar:leshan.jar com.ibm.lwm2m.client.LwM2MAdvClient
//in windows
$ java -classpath ".;LwM2MExampleClient.jar;..\leshan.jar;..\org.eclipse.paho.client.mqttv3-1.0.0.jar" com.ibm.lwm2m.client.LwM2MExampleClient
//in Linux
$ java -classpath ".:LwM2MExampleClient.jar:..\leshan.jar:..\org.eclipse.paho.client.mqttv3-1.0.0.jar" com.ibm.lwm2m.client.LwM2MExampleClient
```

After the client starts, following commands will be displayed
```shell
List of available commands
 register :: Register this client to server
 deregister :: deregister this client from the server
 update-register :: updates the registeration
 update :: (update <resource-id> <value>) update a local resource value
 get :: (get <resource-id>) get a local resource value
 >> Available Object and resource
 >> IPSO temperature Object - <3303/0>
 >> 3303/0/5700 - SensorValue
 >> 3303/0/5601 - Minimum Measured Value
 >> 3303/0/5602 - Maximum Measured Value
 >> 3303/0/5603 - Min Range Value
 >> 3303/0/5604 - Max Range Value
Enter the command
```

* register - Run this command to register the device
* deregister - Run this command to remove this device's registeration
* update-register - Run this command to update this device's registeration
* update - update the local resource value of this device. In this example, we update the sensor value(3303/0/5700) to 43
```shell
    Enter the command
    update 3303/0/5700 43
```
* get - get the local resource value of this device. In this example, we get the sensor value(3303/0/5700)
```shell
    Enter the command
    get 3303/0/5700
    43
```

All the updates that you do to the local resource can be viewed in the server's dashboard. So if you are running this client program from Raspberry Pi, you can view the live temperature. 

---
### Todo's
 - Bootstrap Interface
 - Security
 - Support TLV, Opaque and JSON message format
 - Binding MQTT in Leshan client
 - Write Tests
 - Add Code Comments

---
###License
 - Eclipse Distribution License
 - Eclipse Public License 

---
[http://localhost:8080]:http://localhost:8080
[ant]:http://ant.apache.org/
[eclipse]:http://www.eclipse.org/
[Paho Client]:https://eclipse.org/paho/clients/java/
[Leshan]:https://github.com/eclipse/leshan
[Open Source Mosquitto Broker]:http://iot.eclipse.org/sandbox.html
[git repository]:http://github.com/sathipal/lwm2m_over_mqtt
