package com.ibm.lwm2m.utils;

public interface ClientReportListener {
    boolean reportAndWaitforRep(String dpName, String type, String version, int status, int updstatus, int errcode);
}
