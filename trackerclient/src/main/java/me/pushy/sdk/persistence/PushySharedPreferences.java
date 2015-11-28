package me.pushy.sdk.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import me.pushy.sdk.json.PushySingleton;

public class PushySharedPreferences {
    public static String getToken(Context context) {
        SharedPreferences preferences = PushySingleton.getSettings(context);
        return preferences.getString("pushyToken", null);
    }

    public static void setToken(String token, Context context) {
        SharedPreferences preferences = PushySingleton.getSettings(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("pushyToken", token);
        editor.commit();
    }
}

