package com.ibm.lwm2m.utils;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class ClientReportRegistry {
    private final List<ClientReportListener> listeners = new CopyOnWriteArrayList<>();

    public void notifySend(String dpName, String type, String version, int status, int updstatus, int errcode){
        for (ClientReportListener l : listeners) {
            l.reportAndWaitforRep(dpName, type, version, status, updstatus, errcode);
        }
    }

    public void addListener(ClientReportListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClientReportListener listener) {
        listeners.remove(listener);
    }
}
