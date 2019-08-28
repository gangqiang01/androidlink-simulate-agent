#!/bin/bash
java -classpath ".:./jars/LwM2MAdvClient.jar:./slf4j-api-1.7.25.jar:./log4j-1.2.17.jar:./org.eclipse.paho.client.mqttv3-1.2.1.jar:./commons-net-3.6.jar:./slf4j-log4j12-1.7.21.jar:./fastjson-1.2.47.jar" com.ibm.lwm2m.client.LwM2MAdvClient

