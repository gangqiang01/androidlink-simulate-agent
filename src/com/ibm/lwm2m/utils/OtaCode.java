package com.adv.client.utils;

public class OtaCode {

    /* OTA Status */
    public static int OTA_STATUS_IDLE = 0;
    public static int OTA_STATUS_DOWNLOADING = 1;
    public static int OTA_STATUS_DOWNLOADED = 2;
    public static int OTA_STATUS_UPDATEING = 3;

    /* Update status */
    //Initial value
    public static int OTA_UPDATE_INITIAL = 0;
    // Firmware updated successfully
    public static int OTA_UPDATE_SUCCESS = 1;
    // Not enough flash memory
    public static int OTA_UPDATE_LACK_MEMORY = 2;
    // Out of RAM  during downloading process
    public static int OTA_UPDATE_OUT_OF_RAM = 3;
    // Connection lost during downloading process
    public static int OTA_UPDATE_CONNECTION_LOST = 4;
    // Integrity check failure for new downloaded package
    public static int OTA_UPDATE_CHECK_FAILED = 5;
    // Unsupported package type
    public static int OTA_UPDATE_UNSUPPORTED_TYPE = 6;
    // Invalid URI
    public static int OTA_UPDATE_INVALID_URI = 7;
    // Firmwaree update failed
    public static int OTA_UPDATE_FAILED = 8;
    // Unsupported protocol
    public static int OTA_UPDATE_UNSUPPORTED_PROTOCOL = 9;

    /* OTA protocol*/
    public static int OTA_PROTOCOL_HTTP = 2;
    public static int OTA_PROTOCOL_HTTPS = 3;
    public static int OTA_PROTOCOL_FTP = 6;//extend

}