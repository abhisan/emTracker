package me.pushy.sdk.manager;

import android.content.Context;

import me.pushy.sdk.config.PushyAction;
import me.pushy.sdk.persistence.PushyPersistance;
import me.pushy.sdk.services.PushySocketService;

public class PushyServiceManager {
    public static void startSocketService(Context context) {
        if (PushyPersistance.getToken(context) == null) {
            return;
        }
        PushySocketService.performAction(context, PushyAction.ACTION_START);
    }
}

