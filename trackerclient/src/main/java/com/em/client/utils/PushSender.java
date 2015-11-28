package com.em.client.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushSender {
    public void sendSamplePush(List<String> registrationIDs) {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("message", "Hello World!");
        PushyPushRequest push = new PushyPushRequest(payload, registrationIDs.toArray(new String[registrationIDs.size()]));
        try {

        } catch (Exception exc) {
            System.out.println(exc.toString());
        }
    }
}
