package com.ibm.lwm2m.utils;

public class CommTrans {
    static String deviceId;
    public static void setDeviceId(String deviceId){
        CommTrans.deviceId = deviceId;
    }
    public static String getDeviceId(){
        return deviceId;
    }
}