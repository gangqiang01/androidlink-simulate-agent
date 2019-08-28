package com.ibm.lwm2m.objects;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.ibm.lwm2m.utils.SystemUtil;
import com.ibm.lwm2m.ota.OTAServerManager;
import com.adv.client.utils.OtaCode;
import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.MQTTResource;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.ResponseCode;
import com.ibm.lwm2m.utils.ClientReportRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.min;

public class OtaObject extends MQTTResource {

    private static final Logger LOG = LoggerFactory.getLogger(OtaObject.class);
    public static final String RESOURCE_NAME = "5";

    public final String propertyTypeName = "property";
    public final String bspTypeName = "sysbsp";
    public final String defDeployName = "deploy1";
    public String singleDeployName = null;
    public String singleVersion = null;


    private boolean bInstance = false;
    // Create an object model
    private StringResource packageResource;//common "0"
    private StringResource packageUriResource;//common "1"
    private ExecResource otaExecResource;//common "2"
    private IntegerResource stateResource;//common "3"
    private IntegerResource updateStateResource;//common "5"
    private StringResource packageNameResource;//common "6"
    private StringResource packageVersionResource;//common "7"
    private IntegerResource updateProtocolResource;//common "8"
    private IntegerResource deliveryMethodResource;//common "9"

    //for property value
    private String pdefaultPackageName = propertyTypeName;
    private String pdefaultPackageVersion = "1.0.0";
    private int pdefaultUpdateProtocol = 0;
    private int pdefaultDeliveryMethod = 0;
    private String pdefaultPackage = "ROM_7421_android6.0";
    private String pdefaultPackageUri = "/Jenkins/elaa_7421_android6.0-66/images/elaa_7421_android6.0-66.zip";

    //for bsp value
    private String bdefaultPackageName = defDeployName;
    private String bdefaultPackageVersion = SystemUtil.getBspVersion();
    private int bdefaultUpdateProtocol = 2;
    private int bdefaultDeliveryMethod = 0;
    private String bdefaultPackage = SystemUtil.getSystemBoard();
    private String bdefImageServer = "http://172.21.73.109/androidbsp";

    public OtaObject(String name, boolean bInstance) {//only one
        super(name);
        this.bInstance = bInstance;
        /* Create resources only if its a instance */
        if(bInstance == true) {
            switch(name) {
                case "0": // property
                    packageResource = new StringResource("0", true, this.pdefaultPackage);//string
                    packageUriResource = new StringResource("1", true, this.pdefaultPackageUri);//string
                    //otaExecResource = new UpdatePropertyResource("2");
                    stateResource = new IntegerResource("3", false, 0);//integer
                    updateStateResource = new IntegerResource("5", false);//integer
                    packageNameResource = new StringResource("6", true, this.pdefaultPackageName);//string
                    packageVersionResource = new StringResource("7", true, this.pdefaultPackageVersion);//string
                    updateProtocolResource = new IntegerResource("8", true, this.pdefaultUpdateProtocol);//integer
                    deliveryMethodResource = new IntegerResource("9", true, this.pdefaultDeliveryMethod);//integer
                    break;
                case "1": //bsp
                    packageResource = new StringResource("0", true, this.bdefaultPackage);//string
                    packageUriResource = new StringResource("1", true, this.bdefImageServer);//string
                    otaExecResource = new UpdateAndroidbspResource("2");
                    stateResource = new IntegerResource("3", false, 0);//integer
                    updateStateResource = new IntegerResource("5", false);//integer
                    packageNameResource = new StringResource("6", true, this.bdefaultPackageName);//string
                    packageVersionResource = new StringResource("7", true, this.bdefaultPackageVersion);//string
                    updateProtocolResource = new IntegerResource("8", true, this.bdefaultUpdateProtocol);//integer
                    deliveryMethodResource = new IntegerResource("9", true, this.bdefaultDeliveryMethod);//integer
                    break;
                default: // property
                    packageResource = new StringResource("0", true, this.pdefaultPackage);//string
                    packageUriResource = new StringResource("1", true, this.pdefaultPackageUri);//string
                    //otaExecResource = new UpdatePropertyResource("2");
                    stateResource = new IntegerResource("3", false, 0);//integer
                    updateStateResource = new IntegerResource("5", false);//integer
                    packageNameResource = new StringResource("6", true, this.pdefaultPackageName);//string
                    packageVersionResource = new StringResource("7", true, this.pdefaultPackageVersion);//string
                    updateProtocolResource = new IntegerResource("8", true, this.pdefaultUpdateProtocol);//integer
                    deliveryMethodResource = new IntegerResource("9", true, this.pdefaultDeliveryMethod);//integer
                    break;
            }

        }
    }

    private class UpdateAndroidbspResource extends ExecResource implements OTAServerManager.OTAStateChangeListener {
        private boolean isRunning = false;//our androidbsp otaprocess is singleton
        OTAServerManager mOTAManager;
        // update status defination
        final int UPD_STATUS_IDLE = 0;
        final int UPD_STATUS_FAIL = 1;
        final int UPD_STATUS_SUCCESS = 2;
        //
        int mState = 0;

        public UpdateAndroidbspResource(String name) {
            super(name);
        }

        @Override
        public void handlePUT(MQTTExchange exchange) {
            //
            String[] parameters = exchange.getRequest().getPayloadText().split(" ", 2);
            String content = parameters[1];
            JSONObject jsonObj = JSON.parseObject(content);
            //
            String deployName = jsonObj.getString("dpname");
            packageNameResource.setValue(deployName);
            String baseURI = jsonObj.getString("bspimageurl");
            packageUriResource.setValue(baseURI);
            String productName = packageResource.getValue();
            String version = jsonObj.getString("version");
            packageVersionResource.setValue(version);
            if (this.isRunning == true) { // An instance has run
                LOG.info("bspota, an instance has run");
                exchange.respond(ResponseCode.SERVER_ERROR, "an instance has run");
                return;
            } else {
                // begin, set isRunning to true
                isRunning = true;
                int proto = Integer.parseInt(updateProtocolResource.getValue());
                String URI = baseURI + "/";

                LOG.info("bspota, baseURI: " + baseURI + ",dpname: " + deployName + ",product: " + productName + ",version: " + version + ",protocol: " + proto);
                if(proto != 2){//now only support http protocol
                    LOG.info("bspota, protocl validate failed, need: http, recv: " + proto);
                    exchange.respond(ResponseCode.SERVER_ERROR, "protocl validate failed");
                    isRunning = false;
                    return;
                }

                String localVersion = SystemUtil.getBspVersion();
                if(!SystemUtil.versionIsNew(version, localVersion)){
                    LOG.info("bspota, version of device is newer");
                    exchange.respond(ResponseCode.SERVER_ERROR, "version of device is newer");
                    isRunning = false;
                    return;
                }

                try {
                    mOTAManager = new OTAServerManager(URI, productName, version);
                } catch (MalformedURLException e) {
                    mOTAManager = null;
                    LOG.info("bspota, meet not a mailformat URL... should not happens." + e);
                    exchange.respond(ResponseCode.SERVER_ERROR, "internal error");
                    isRunning = false;
                    return;
                }
                mOTAManager.setmListener(this);

                if(mOTAManager.checkServer()) {
                    exchange.respond(ResponseCode.CHANGED, "UpdateExec successfully executed");
                    singleDeployName = deployName;
                    singleVersion = version;
                    // begin async deploy
                    new Thread(new Runnable() {
                        public void run() {
                            mOTAManager.startCheckingVersion();
                        }
                    }).start();
                } else {
                    LOG.info("bspota, try-fetch the packageURL fail");
                    exchange.respond(ResponseCode.SERVER_ERROR, "try-fetch the packageURL fail");
                    isRunning = false;
                    return;
                }
            }
        }

        private void  notifySend(String type, String version, int status, int updstatus, int errcode){
            ClientReportRegistry clientReportRegistry = LwM2MClient.getClientReportRegistry();
            clientReportRegistry.notifySend(singleDeployName, type, version, status, updstatus, errcode);
        }

        public void onStateOrProgress(int message, int error, Object info)
        {
            //LOG.info("onStateOrProgress: " + "message: " + message + " error:" + error + " info: " + info );
            switch (message) {
                case STATE_IN_CHECKED:
                    mState = STATE_IN_CHECKED;
                    LOG.info("mState = " + mState);
                    onStateInChecked(error, info);
                    break;
                case STATE_IN_DOWNLOADING:
                    mState = STATE_IN_DOWNLOADING;
                    LOG.info("mState = " + mState);
                    onStateDownload(error, info);
                    break;
                case STATE_IN_UPGRADING:
                    mState = STATE_IN_UPGRADING;
                    LOG.info("mState = " + mState);
                    onStateUpgrade(error, info);
                    break;
                case MESSAGE_DOWNLOAD_PROGRESS:
                case MESSAGE_VERIFY_PROGRESS:
                    onProgress(message, error, info);
                    break;
            }
        }
        void onStateInChecked(int error, Object info) {
            String version = singleVersion;
            int updstate = UPD_STATUS_FAIL;
            if (error == 0) {
                // return no error, usually means have a version info from remote server, release name is in @info
                // needs check here whether the local version is newer then remote version
                if (mOTAManager.compareLocalVersionToServer() == false) {
                    // we are already latest...
                    LOG.info("onStateInChecked: we are already latest");
                    notifySend(bspTypeName, version, mState, updstate, error);
                    isRunning = false;
                    return;
                } else if (mOTAManager.compareLocalVersionToServer() == true ) {
                    new Thread(new Runnable() {
                        public void run() {
                            mOTAManager.startDownloadUpgradePackage();
                        }
                    }).start();
                    LOG.info("onStateInChecked: start downloading...");
                }
            } else if (error == ERROR_WIFI_NOT_AVALIBLE) {
                LOG.info("onStateInChecked: wifi not avalible");
                notifySend(bspTypeName, version, mState, updstate, error);
                isRunning = false;
            } else if (error == ERROR_CANNOT_FIND_SERVER) {
                LOG.info("onStateInChecked: cannot find server");
                notifySend(bspTypeName, version, mState, updstate, error);
                isRunning = false;
            } else if (error == ERROR_WRITE_FILE_ERROR ) {
                LOG.info("onStateInChecked: write file error");
                notifySend(bspTypeName, version, mState, updstate, error);
                isRunning = false;
            }
        }
        void onStateDownload(int error, Object info) {
            String version = singleVersion;
            int updstate = UPD_STATUS_FAIL;
            if (error == ERROR_CANNOT_FIND_SERVER) {
                // in this case, the build.prop already found but the server don't have upgrade package
                // report as "Server Error: Not have upgrade package";
                LOG.info("onStateDownload: can not find server");
                notifySend(bspTypeName, version, mState, updstate, error);
                isRunning = false;
            } else if (error == ERROR_WRITE_FILE_ERROR) {
                LOG.info("onStateDownload: write file error");
                notifySend(bspTypeName, version, mState, updstate, error);
                isRunning = false;
            }

            if (error == 0) {
                // success download, let try to start with install package...
                // we should already in another thread, no needs to create a thread.
                LOG.info("onStateDownload: start installing...");
                mOTAManager.startInstallUpgradePackage();
            }
        }
        void onStateUpgrade(int error, Object info) {
            String version = singleVersion;
            int updstate = UPD_STATUS_FAIL;
            if (error == ERROR_PACKAGE_VERIFY_FAILED) {
                LOG.info("onStateUpgrade: package verify failed, signaure not match");
                notifySend(bspTypeName, version, mState, updstate, error);
                isRunning = false;
                // meet error in Verify, fall back to check.
                // TODO which state should ?
            } else if (error == ERROR_PACKAGE_INSTALL_FAILED) {
                LOG.info("onStateUpgrade: package install failed");
                notifySend(bspTypeName, version, mState, updstate, error);
                isRunning = false;
            } else if (error == 0){// upgrade success
                LOG.info("onStateUpgrade: upgrade success");
                notifySend(bspTypeName, version, mState, UPD_STATUS_SUCCESS, error);
                // finish, so set isRunning to false
                isRunning = false;
                saveProcess(error);
            }
        }
        void onProgress(int message, int error, Object info) {
            final Long progress = new Long((Long)info);

            //LOG.info("onProgress, progress : " + progress);
            if (message == MESSAGE_DOWNLOAD_PROGRESS) {
                //LOG.info("onProgress, is downloading");
            } else if (message == MESSAGE_VERIFY_PROGRESS) {
                //LOG.info("onProgress, is verifying");
            }
        }
        void saveProcess(int error){
            int updstate = UPD_STATUS_IDLE;
            if(error == 0){
                // saveProcess to sqlite db
                ;
            }
        }
    }

    public static OtaObject createObject() {
        /* ota root resource */
        OtaObject to = new OtaObject(RESOURCE_NAME, false);
        LwM2MClient.getRootResource().add(to);
        return to;
    }

    public static OtaObject createObjectInstance(int id) {//0: property; 1: androidbsp; 2: other
        String strInstance = Integer.toString(id);
        /* ota instance 0 resource */
        OtaObject to = new OtaObject(strInstance, true);
        /* ota child resource */
        Resource resource = LwM2MClient.getRootResource().getChild(RESOURCE_NAME);
        resource.add(to);
        to.add(to.packageResource);
        to.add(to.packageUriResource);
        to.add(to.otaExecResource);
        to.add(to.stateResource);
        to.add(to.updateStateResource);
        to.add(to.packageNameResource);
        to.add(to.packageVersionResource);
        to.add(to.updateProtocolResource);
        to.add(to.deliveryMethodResource);
        return to;
    }

}
