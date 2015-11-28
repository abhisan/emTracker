package me.pushy.sdk.receivers.em;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import me.pushy.sdk.manager.em.EmPushyServiceManager;

public class EmPushyBootReceiver
        extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Log.d((String) "Pushy", (String) "Device booted");
        EmPushyServiceManager.startSocketService(context);
    }
}

