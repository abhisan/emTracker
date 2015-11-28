package me.pushy.sdk.services;
import android.os.Binder;

public class MqttServiceBinder extends Binder {
    private EmPushySocketService mqttService;
    private String activityToken;

    MqttServiceBinder(EmPushySocketService mqttService) {
        this.mqttService = mqttService;
    }

    public EmPushySocketService getService() {
        return this.mqttService;
    }

    void setActivityToken(String activityToken) {
        this.activityToken = activityToken;
    }

    public String getActivityToken() {
        return this.activityToken;
    }
}