package me.pushy.sdk.manager.em;

import android.content.Context;
import android.content.Intent;

import me.pushy.sdk.config.PushyAction;
import me.pushy.sdk.persistence.PushyPersistance;
import me.pushy.sdk.services.EmPushySocketService;
import me.pushy.sdk.services.PushySocketService;

public class EmPushyServiceManager {
    public static Intent startSocketService(Context context) {
        if (PushyPersistance.getToken(context) == null) {
            return null;
        }
        return EmPushySocketService.performAction(context, PushyAction.ACTION_START);
    }
}

