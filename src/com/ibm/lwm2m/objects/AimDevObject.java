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
import com.ibm.lwm2m.utils.SystemUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AimDevObject extends MQTTResource {
    private static final Logger LOG = LoggerFactory.getLogger(AimDevObject.class);

    public static final String RESOURCE_NAME = "40001";

    private boolean bInstance = false;

    // Create an object model
    private BooleanResource isWifiResource;
    private BooleanResource isBluetoothResource;
    private StringResource getSetBrightness;
    private StringResource getSystemVersion;
    private StringResource getBoardName;
    private StringResource getSystemModel;
    private StringResource getAgentVersion;
    private StringResource getBspversion;
    private StringResource getPlatformName;


    public AimDevObject(String name, boolean bInstance) {
        super(name);
        this.bInstance = bInstance;
        /* Create resources only if its a instance */
        if(bInstance == true) {
            isWifiResource = new BooleanResource("27000", true);
            isBluetoothResource = new BooleanResource("27001", true);
            getSetBrightness = new StringResource("27002", true, "110");
            getSystemVersion = new StringResource("27003", false, SystemUtil.getSystemVersion());
            getBoardName = new StringResource("27004", false, SystemUtil.getSystemBoard());
            getSystemModel = new StringResource("27005", false, SystemUtil.getSystemModel());
            getAgentVersion = new StringResource("27006", false, SystemUtil.getAgentVersion());
            getBspversion = new StringResource("27007", false, SystemUtil.getBspVersion());
            getPlatformName = new StringResource("27008", false, SystemUtil.getPlatformName());
        }
    }

    public static AimDevObject createObject() {
        //1. add object id
        AimDevObject to = new AimDevObject(RESOURCE_NAME, false);
        LwM2MClient.getRootResource().add(to);
        return to;
    }

    public static AimDevObject createObjectInstance() {
        //2. add instance id
        AimDevObject to = new AimDevObject("0", true);
        Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
        resource.add(to);
        //3. add resource id
        to.add(to.isWifiResource);
        to.add(to.isBluetoothResource);
        to.add(to.getSetBrightness);
        to.add(to.getSystemVersion);
        to.add(to.getBoardName);
        to.add(to.getSystemModel);
        to.add(to.getAgentVersion);
        to.add(to.getBspversion);
        to.add(to.getPlatformName);
        return to;
    }

}