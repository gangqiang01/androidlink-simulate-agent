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

import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.ResponseCode;
import com.ibm.lwm2m.utils.SystemUtil;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExtendInfoObject extends MQTTResource {
    private static final Logger LOG = LoggerFactory.getLogger(ExtendInfoObject.class);

    public static final String RESOURCE_NAME = "40006";

    private boolean bInstance = false;

    // Create an object model
    private GetCpuLoadingResource getCpuLoadingResource;


    public ExtendInfoObject(String name, boolean bInstance) {
        super(name);
        this.bInstance = bInstance;
        /* Create resources only if its a instance */
        if(bInstance == true) {
            getCpuLoadingResource = new GetCpuLoadingResource("27500", false);
        }
    }

    private class GetCpuLoadingResource extends StringResource {

        public GetCpuLoadingResource(String name, boolean bWrite) {
            super(name, bWrite);
        }

        @Override
        public String getValue() {
            String value;
            Random r = new Random();
            int Low = 20;
            int High = 50;
            int Result = r.nextInt(High-Low) + Low;
            //value = SystemUtil.getCpuUsageStatistic();
            value = Integer.toString(Result);
            return value;
        }
    }


    public static ExtendInfoObject createObject() {
        //1. add object id
        ExtendInfoObject to = new ExtendInfoObject(RESOURCE_NAME, false);
        LwM2MClient.getRootResource().add(to);
        return to;
    }

    public static ExtendInfoObject createObjectInstance() {
        //2. add instance id
        ExtendInfoObject to = new ExtendInfoObject("0", true);

        Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
        resource.add(to);
        //3. add resource id
        to.add(to.getCpuLoadingResource);
        //it may cause app restart
        //to.add(to.isLargetextKeyResource);
        return to;
    }

}