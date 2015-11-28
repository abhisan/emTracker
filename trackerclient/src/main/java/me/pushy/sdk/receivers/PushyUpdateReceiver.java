package me.pushy.sdk.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import me.pushy.sdk.manager.PushyServiceManager;

public class PushyUpdateReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Log.d((String) "Pushy", (String) "App updated");
        PushyServiceManager.startSocketService(context);
    }
}

