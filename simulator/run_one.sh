#!/bin/bash
java -classpath ".:./jars/LwM2MAdvClient.jar:./jars/slf4j-api-1.7.25.jar:./jars/log4j-1.2.17.jar:./jars/org.eclipse.paho.client.mqttv3-1.2.1.jar:./jars/commons-net-3.6.jar:./jars/slf4j-log4j12-1.7.21.jar:./jars/fastjson-1.2.47.jar" com.ibm.lwm2m.client.LwM2MAdvClient
