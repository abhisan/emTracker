package me.pushy.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.em.client.utils.EmUtils;

import me.pushy.sdk.exceptions.PushyException;
import me.pushy.sdk.json.PushySingleton;
import me.pushy.sdk.manager.PushyServiceManager;
import me.pushy.sdk.permissions.PushyPermissionVerification;
import me.pushy.sdk.persistence.PushyPersistance;

public class Pushy {
    public static void listen(Context context) {
        PushyServiceManager.startSocketService(context);
    }

    public static void setHeartbeatInterval(long interval, Context context) {
        SharedPreferences preferences = PushySingleton.getSettings(context);
        Editor editor = preferences.edit();
        editor.putLong("pushyHeartbeatInterval", interval);
        editor.commit();
    }

    public static void startPushy(Context context, String topic) throws PushyException {
        PushyPermissionVerification.verifyManifestPermissions(context);
        String clientId = PushyPersistance.getToken(context);
        clientId = EmUtils.shortUUID();
        PushyPersistance.saveToken(clientId, context);
        PushyPersistance.saveTopic(topic, context);
        listen(context);
    }
}