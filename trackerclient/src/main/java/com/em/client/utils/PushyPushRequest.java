package com.em.client.utils;

public class PushyPushRequest {
    public Object data;
    public String[] registration_ids;

    public PushyPushRequest(Object data, String[] registrationIDs) {
        this.data = data;
        this.registration_ids = registrationIDs;
    }
}