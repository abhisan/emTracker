/*
 * Decompiled with CFR 0_102.
 */
package me.pushy.sdk.config;

public class PushySocket {
    public static final int PORT = 1884;
    public static final String ENDPOINT = "tcp://104.200.17.97";
    //public static final String ENDPOINT = "tcp://192.84.45.43";
    public static final int QOS = 1;
    public static final long INITIAL_RETRY_INTERVAL = 500L;
    public static final long MAXIMUM_RETRY_INTERVAL = 60000L;
    public static final boolean MQTT_CLEAN_START = true;
    public static final short MQTT_KEEP_ALIVE = 301;
    public static final int MQTT_QUALITY_OF_SERVICE = 1;
    public static final boolean MQTT_RETAINED_PUBLISH = false;
}

