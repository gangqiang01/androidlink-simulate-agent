package com.ibm.lwm2m.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.util.Random;
import com.ibm.lwm2m.utils.SystemUtil;

import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.net.SocketException;

/**
 * Created by root on 19-4-1.
 */
public class SystemUtil {
    private static final Logger LOG = LoggerFactory
            .getLogger(SystemUtil.class);
    private static String SEPARATOR_OF_MAC = ":";
    private static String randomMac4Qemu() {
        String macaddr = null;
        int i = 0;
        StringBuffer sb = new StringBuffer();
        Random random = new Random();
        String[] mac = {
                String.format("%02x", 0x52),
                String.format("%02x", 0x54),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff))
        };

        for(i=0; i<5; i++){
            sb.append(mac[i]);
            sb.append(SEPARATOR_OF_MAC);
        }
        sb.append(mac[5]);

        return sb.toString();
    }
    public static String getMACAddress() {
        return randomMac4Qemu();
    }

    // now don't work, but "ls -l" is working
    public static String getCpuUsageStatistic() {
        String[] cmdLine = {
                "/bin/sh",
                "-c",
                "top -n 1 |grep Cpu | cut -d \",\" -f 1 | cut -d \":\" -f 2 | cut -d 'u' -f 1"
        };
        String tempString = executeShellOneLine(cmdLine);
        //LOG.error("cpu loading: " + tempString);
        return tempString.trim();
    }
    private static String executeShellOneLine(String[] cmdLine) {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(cmdLine);

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                //LOG.info("## op: " + line);
                output.append(line);
            }
            p.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }
    public static String getIP() {
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                //System.out.println(netInterface.getName());
                if(!(netInterface.getName().contains("eth") || netInterface.getName().contains("enp")))
                    continue;
                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = (InetAddress) addresses.nextElement();
                    if (ip != null && ip instanceof Inet4Address) {
                        System.out.println("local IP = " + ip.getHostAddress());
                        return ip.getHostAddress();
                    }
                }
            }
            return "0.0.0.0";
        } catch (SocketException e) {
            e.printStackTrace();
            return "0.0.0.0";
        }
    }
    public static String getSystemBoard(){
        return "rom-3420-a1";
    }
    public static String getSystemVersion() {
        return "Android6.0.1";
    }
    public static String getManufacture(){
        return "simulator-device";
    }
    public static String getSystemModel() {
        return "simulator-device";
    }
    public static String getBspVersion() {
        return "eng.root.20181227.135835";
    }
    public static String getPlatformName() {return "imx6"; }
    public static String getAgentVersion() {return "v1.0.0"; }

    private static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if(!isNum.matches())
            return false;
        return true;
    }
    //version eg:  eng.root.20180913.144510/20180913.144510
    public static boolean versionIsNew(String version, String baseVersion){
        String ver1 = null;
        String ver2 = null;
        if(version == null || version.equals("") || baseVersion == null || baseVersion.equals(""))
            return false;
        ver1 = version.trim();
        ver2 = baseVersion.trim();
        String[] ver1a = ver1.split("\\.");
        String[] ver2a = ver2.split("\\.");
        int len1 = ver1a.length;
        int len2 = ver2a.length;
        int len;
        if(len1 >= len2)
            len = len2;
        else
            len = len1;
        for(int i=0; i<len; i++){
            if(ver1a[i].equals("") || ver2a[i].equals("")) {
                return false;
            }
            if(!isNumeric(ver1a[i]) && !isNumeric(ver2a[i])){
                if(ver1a[i].equals(ver2a[i])){
                    continue;
                }
                else {
                    //return false;
                    continue;// skip unNumber compare
                }
            }
            if(Integer.parseInt(ver1a[i]) == Integer.parseInt(ver2a[i]))
                continue;
            else if(Integer.parseInt(ver1a[i]) > Integer.parseInt(ver2a[i])) {
                return true;
            } else {
                return false;
            }
        }
        //equals, return true
        return true;
    }

    public static String getFileMD5(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) > 0) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    /***
     * Get MD5 of one file！test ok!
     *
     * @param filepath
     * @return
     */
    public static String getFileMD5(String filepath) {
        File file = new File(filepath);
        return getFileMD5(file);
    }

}
