package com.ibm.lwm2m.utils;


import org.apache.commons.net.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.UnsupportedEncodingException;


public class DESUtil {
    private static final String KEY_ALGORITHM = "DES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "DES/ECB/PKCS5Padding";
    private static String secretKey = "ahc*5f/8";
    /**
     * encrypt
     * @param data
     * @return
     */
    private static byte[] encrypt(byte[] data) {
        try {
            byte[] key = secretKey.getBytes();
            // 初始化向量
            IvParameterSpec iv = new IvParameterSpec(key);
            DESKeySpec desKey = new DESKeySpec(key);
            // 创建一个密匙工厂，然后用它把DESKeySpec转换成securekey
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            // Cipher对象实际完成加密操作
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            // 用密匙初始化Cipher对象
            cipher.init(Cipher.ENCRYPT_MODE, securekey, iv);
            // 现在，获取数据并加密
            // 正式执行加密操作
            return cipher.doFinal(data);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     * @param src
     * @return
     * @throws Exception
     */
    private static byte[] decrypt(byte[] src) {
        try {
            byte[] key = secretKey.getBytes();
            // 初始化向量
            IvParameterSpec iv = new IvParameterSpec(key);
            // 创建一个DESKeySpec对象
            DESKeySpec desKey = new DESKeySpec(key);
            // 创建一个密匙工厂
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            // 将DESKeySpec对象转换成SecretKey对象
            SecretKey securekey = keyFactory.generateSecret(desKey);
            // Cipher对象实际完成解密操作
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            // 用密匙初始化Cipher对象
            cipher.init(Cipher.DECRYPT_MODE, securekey, iv);
            // 真正开始解密操作
            return cipher.doFinal(src);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static String encrypt(String srcStr) {
        try {
            byte[] src = srcStr.getBytes("UTF-8");
            byte[] buf = encrypt(src);
            //return parseByte2HexStr(buf);
            return Base64.encodeBase64String(buf);
        } catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return null;
    }
    public static String decrypt(String hexStr) {
        try {
            //byte[] src = parseHexStr2Byte(hexStr);
            byte[] src = Base64.decodeBase64(hexStr);
            byte[] buf = decrypt(src);
            return new String(buf, "UTF-8");
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}