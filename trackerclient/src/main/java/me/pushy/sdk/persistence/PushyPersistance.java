package me.pushy.sdk.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import me.pushy.sdk.config.PushyStorage;
import me.pushy.sdk.io.PushyIO;
import me.pushy.sdk.json.PushySingleton;

public class PushyPersistance {
    public static String getToken(Context context) {
        String token = PushyPersistance.getTokenFromSharedPreferences(context);
        if (token == null) {
            try {
                token = PushyPersistance.getTokenFromExternalStorage(context);
            } catch (Exception exc) {
                Log.d((String) "Pushy", (String) ("Get token from external storage failed: " + exc.getMessage()));
            }
        }
        return token;
    }
    public static void saveToken(String token, Context context) {
        PushyPersistance.saveTokenInSharedPreferences(token, context);
        try {
            PushyPersistance.saveTokenInExternalStorage(token, context);
        } catch (Exception exc) {
            Log.d((String) "Pushy", (String) ("Saving token to external storage failed: " + exc.getMessage()));
        }
    }
    public static String  getTopic(Context context) {
        SharedPreferences preferences = PushySingleton.getSettings(context);
        return preferences.getString("pushyTopic", null);
    }
    public static void saveTopic(String  topic, Context context) {
        SharedPreferences preferences = PushySingleton.getSettings(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("pushyTopic", topic);
        editor.commit();
    }
    private static String getTokenFromExternalStorage(Context context) throws Exception {
        String path = PushyPersistance.getTokenExternalStoragePath(context);
        return PushyIO.readFromFile(path);
    }

    public static String getTokenExternalStoragePath(Context context) throws Exception {
        return PushyStorage.EXTERNAL_STORAGE_DIRECTORY + context.getPackageName() + "/" + "registration.id";
    }

    public static String getTokenFromSharedPreferences(Context context) {
        SharedPreferences preferences = PushySingleton.getSettings(context);
        return preferences.getString("pushyToken", null);
    }
    public static void saveTokenInSharedPreferences(String token, Context context) {
        SharedPreferences preferences = PushySingleton.getSettings(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("pushyToken", token);
        editor.commit();
    }
    public static void saveTokenInExternalStorage(String token, Context context) throws Exception {
        String path = PushyPersistance.getTokenExternalStoragePath(context);
        PushyIO.writeToFile(path, token);
    }
}