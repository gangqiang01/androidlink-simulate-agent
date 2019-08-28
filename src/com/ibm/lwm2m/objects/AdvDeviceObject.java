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

public class AdvDeviceObject extends MQTTResource {

    public static final String RESOURCE_NAME = "3";

    private boolean bInstance = false;

    // Create an object model
    private StringResource manufacturerResource;
    private StringResource modelResource;
    private StringResource serialNumberResource;
    private StringResource firmwareResource;
    private RebootResource rebootResource;
    private ExecResource factoryResetResource;
    private IntegerMultipleResource powerAvailablePowerResource;
    private IntegerMultipleResource powerSourceVoltageResource;
    private IntegerMultipleResource powerSourceCurrentResource;
    private IntegerResource batteryLevelResource;
    private MemoryFreeResource memoryFreeResource;
    private IntegerMultipleResource errorCodeResource;
    private ExecResource resetErrorCodeResource;
    private TimeResource currentTimeResource;
    private StringResource utcOffsetResource;
    private StringResource timezoneResource;
    private StringResource bindingsResource;
    private MemoryTotalResource memoryTotalResource;


    public AdvDeviceObject(String name, boolean bInstance) {
        super(name);
        this.bInstance = bInstance;

		/* Create resources only if its a instance */
        if(bInstance == true) {
            manufacturerResource = new StringResource("0", false, SystemUtil.getManufacture());
            modelResource = new StringResource("1", false, SystemUtil.getSystemModel());
            serialNumberResource = new StringResource("2", false, SystemUtil.getMACAddress());
            firmwareResource = new StringResource("3", false, "1.0.0");
            rebootResource = new RebootResource("4");
            factoryResetResource = new ExecResource("5");
            powerAvailablePowerResource = new IntegerMultipleResource("6", false, new int[] { 0, 4 });
            powerSourceVoltageResource = new IntegerMultipleResource("7", false, new int[] { 12000,
                    5000 });
            powerSourceCurrentResource = new IntegerMultipleResource("8", false, new int[] { 150, 75 });
            batteryLevelResource = new IntegerResource("9", false, 92);
            memoryFreeResource = new MemoryFreeResource("10", false);
            errorCodeResource = new IntegerMultipleResource("11", false, new int[] { 0 });
            resetErrorCodeResource = new ExecResource("12");
            currentTimeResource = new TimeResource("13", true);
            utcOffsetResource = new StringResource("14", true,
                    new SimpleDateFormat("yy-mm-dd").format(Calendar.getInstance().getTime()));
            timezoneResource = new StringResource("15", true, TimeZone.getDefault().getID());
            bindingsResource = new StringResource("16", false, "MQTT");

            memoryTotalResource = new MemoryTotalResource("21", false);

        }
    }


    private class TimeResource extends LongResource {

        public TimeResource(String name, boolean bWrite) {
            super(name, bWrite, new Date().getTime());
        }

        @Override
        public String getValue() {
            return Long.toString(new Date().getTime());
        }
    }

    private class MemoryFreeResource extends IntegerResource {

        public MemoryFreeResource(String name, boolean bWrite) {
            super(name, bWrite);
        }

        @Override
        public String getValue() {
            return Long.toString(Runtime.getRuntime().freeMemory());
        }
    }

    private class MemoryTotalResource extends IntegerResource {

        public MemoryTotalResource(String name, boolean bWrite) {
            super(name, bWrite);
        }

        @Override
        public String getValue() {
            return Long.toString(Runtime.getRuntime().totalMemory());
        }
    }

    public static AdvDeviceObject createObject() {

        //1. add object id
        AdvDeviceObject to = new AdvDeviceObject(RESOURCE_NAME, false);
        LwM2MClient.getRootResource().add(to);
        return to;
    }

    public static AdvDeviceObject createObjectInstance() {
        //2. add instance id
        AdvDeviceObject to = new AdvDeviceObject("0", true);

        Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
        resource.add(to);
        to.add(to.manufacturerResource);
        to.add(to.modelResource);
        to.add(to.serialNumberResource);
        to.add(to.firmwareResource);
        to.add(to.rebootResource);
        to.add(to.factoryResetResource);
        to.add(to.powerAvailablePowerResource);
        to.add(to.powerSourceCurrentResource);
        to.add(to.powerSourceVoltageResource);
        to.add(to.batteryLevelResource);
        to.add(to.memoryFreeResource);
        to.add(to.errorCodeResource);
        to.add(to.resetErrorCodeResource);
        to.add(to.currentTimeResource);
        to.add(to.utcOffsetResource);
        to.add(to.timezoneResource);
        to.add(to.bindingsResource);
        to.add(to.memoryTotalResource);
        return to;
    }

    private class RebootResource extends ExecResource {

        public RebootResource(String name) {
            super(name);
        }

        @Override
        public void handlePOST(MQTTExchange exchange) {
            try {

                Runtime r = Runtime.getRuntime();

                exchange.respond(ResponseCode.CHANGED,
                        " Reboot successfully executed");
                // To reboot the Raspi
                r.exec("/system/bin/reboot");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
