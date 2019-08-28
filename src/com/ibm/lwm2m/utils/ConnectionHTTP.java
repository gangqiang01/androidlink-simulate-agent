package com.ibm.lwm2m.utils;

import com.ibm.mqttv3.binding.MQTTExchange;
import com.ibm.mqttv3.binding.MQTTWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.ibm.mqttv3.binding.ResponseCode;

public class ConnectionHTTP {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionHTTP.class);

    private static int timeout = 18000;
    private static int readtimeout = 6000;
    private static final String EXCEPTION_HTTP = "EXCEPTION_HTTP_";


    /**
     * Convert inputStream to String
     * @param stream InputStream to convert
     * @return String converted
     * @throws IOException error
     */
    private static String inputStreamToString(final InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();
        return sb.toString();
    }


    public static void getApkAndInstallAsync2(final String strPam, final MQTTExchange exchange) {

        Thread t = new Thread(new Runnable()
        {
            public void run() {
                OutputStream output = null;
                String url;
                //final String pathFile = "/cache/jin.apk";
                //final String pathFile = "/cache/" + UUID.randomUUID().toString().replace("-", "") + ".apk";
                String apkPath = "/tmp/apk/";
                final String pathFile = apkPath + UUID.randomUUID().toString().replace("-", "") + ".apk";
                File dir = new File(apkPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                try {
                    //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    //StrictMode.setThreadPolicy(policy);

                    url = strPam;
                    URL dataURL = new URL(url);
                    //FlyveLog.d("getSyncFile: " + url);
                    HttpURLConnection conn = (HttpURLConnection) dataURL.openConnection();

                    conn.setConnectTimeout(timeout);
                    conn.setReadTimeout(readtimeout);
                    conn.setInstanceFollowRedirects(true);

                    HashMap<String, String> header = new HashMap();
                    header.put("Accept", "application/octet-stream");
                    header.put("Content-Type", "application/json");

                    for (Map.Entry<String, String> entry : header.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                        //FlyveLog.d(entry.getKey() + " = " + entry.getValue());
                    }

                    int fileLength = conn.getContentLength();

                    InputStream input = conn.getInputStream();
                    output = new FileOutputStream(pathFile);

                    byte[] data = new byte[4096];
                    long total = 0;
                    int count;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        //publish progress only if total length is known
                        if (fileLength > 0) {
                            ;//FlyveLog.v(String.valueOf(((int) (total * 100 / fileLength))));
                        }
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    Runtime.getRuntime().exec("chmod 755 " + pathFile);
                    /* invoke AimSdk APIs */
                    //
                    try {
                        Thread.sleep(20 * 1000);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    exchange.respond(ResponseCode.CHANGED, pathFile);

                    return;
                } catch (final Exception ex) {
                    final String errStr = ex.getClass() + " : " + ex.getMessage();
                    exchange.respond(ResponseCode.SERVER_ERROR, errStr);
                    return;
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (Exception ex) {
                            //FlyveLog.e(ex.getMessage());
                        }
                    }
                }

            }
        });
        t.start();
    }

}
