package com.em.embustracker.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.em.embustracker.AppController;

public class EmUtils {
    public static String url (){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String serverLocation = sharedPrefs.getString("server_location", "");
        String serverPort = sharedPrefs.getString("server_port", "");
        if (serverLocation == null || serverLocation.isEmpty() || serverPort == null || serverPort.isEmpty()) {
            return "";
        }
        String url = "http://" + serverLocation + ":" + serverPort;
        return url;
    }

    public static String route(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String route  = sharedPrefs.getString("route_id", "");
        return route;
    }

    public static String client(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String client  = sharedPrefs.getString("client_id", "");
        return client;
    }
}
