package com.ibm.lwm2m.objects;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.ResponseCode;

import com.ibm.lwm2m.client.LocalResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.mqttv3.binding.MQTT.PUT;
import com.ibm.lwm2m.utils.*;


public class AimAppObject extends MQTTResource {
    private static final Logger LOG = LoggerFactory.getLogger(AimAppObject.class);

    public static final String RESOURCE_NAME = "40004";

    private boolean bInstance = false;

    // Create an object model
    private InstAppResource appInstResource;

    public AimAppObject(String name, boolean bInstance) {
        super(name);
        this.bInstance = bInstance;

        /* Create resources only if its a instance */
        if(bInstance == true) {
            appInstResource = new InstAppResource("27308", true);//string
        }
    }
    public class InstAppResource extends MQTTResource implements LocalResource {
        private String value = "";
        boolean bWrite;

        public InstAppResource(String name, boolean bWrite) {
            super(name);
            this.bWrite = bWrite;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }


        @Override
        public void handlePUT(MQTTExchange exchange) {
            String ptValue;

            String[] parameters = exchange.getRequest().getPayloadText().split(" ", 2);
            if(PUT.value(parameters[0]) == PUT.WRITE && this.bWrite){
                ptValue = parameters[1];
                ConnectionHTTP.getApkAndInstallAsync2(ptValue, exchange);

            } else {
                exchange.respond(ResponseCode.METHOD_NOT_ALLOWED, "");
            }
        }

    }


    public static AimAppObject createObject() {
        //1. add object id
        AimAppObject to = new AimAppObject(RESOURCE_NAME, false);
        LwM2MClient.getRootResource().add(to);
        return to;
    }

    public static AimAppObject createObjectInstance() {
        //2. add instance id
        AimAppObject to = new AimAppObject("0", true);

        Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
        resource.add(to);
        //3. add resource id
        to.add(to.appInstResource);
        return to;
    }

}