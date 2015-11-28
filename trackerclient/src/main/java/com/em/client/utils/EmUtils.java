package com.em.client.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.em.client.AppController;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.nio.ByteBuffer;
import java.util.UUID;

public class EmUtils {
    public static void showProgressDialog(ProgressDialog pDialog) {
        if (pDialog != null && !pDialog.isShowing()) {
            pDialog.setMessage("Loading...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();
        }
    }

    public static void hideProgressDialog(ProgressDialog pDialog) {
        if (pDialog != null && pDialog.isShowing())
            pDialog.dismiss();
    }

    public static String getServerUrl() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String serverLocation = sharedPrefs.getString("server_location", "");
        String serverPort = sharedPrefs.getString("server_port", "");
        if (serverLocation == null || serverLocation.isEmpty() || serverPort == null || serverPort.isEmpty()) {
            return null;
        }
        return "http://" + serverLocation + ":" + serverPort + "/api/";
    }

    public static void clearPreferences() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.commit();
    }

    public static boolean isCredentialSaved() {
        if (getUserId() != null && !getUserId().isEmpty() && getPassword() != null && !getPassword().isEmpty()) {
            return true;
        }
        return false;
    }

    public static String getUserId() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String route = sharedPrefs.getString("userId", "");
        return route;
    }

    public static String getPassword() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String route = sharedPrefs.getString("password", "");
        return route;
    }

    public static void setUserId(String userId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userId", userId);
        editor.commit();
    }

    public static void setPassword(String password) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("password", password);
        editor.commit();
    }

    public static String getIOServerSocketUrl() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String serverLocation = sharedPrefs.getString("server_location", "");
        String serverPort = sharedPrefs.getString("io_server_port", "");
        if (serverLocation == null || serverLocation.isEmpty() || serverPort == null || serverPort.isEmpty()) {
            return null;
        }
        return "http://" + serverLocation + ":" + serverPort;
    }

    public static String getMQTTServerUrl() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
        String serverLocation = sharedPrefs.getString("server_location", "");
        return "tcp://" + serverLocation + "@" + 1884;
    }

    public static boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(AppController.getInstance().getApplicationContext());
        if (status == ConnectionResult.SUCCESS)
            return true;
        return false;
    }

    public static boolean isGPSServiceAvailable() {
        LocationManager locationManager = (LocationManager) AppController.getInstance().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            return true;
        return false;
    }

    private static Boolean isInternetConnectionAvailable() {
        ConnectivityManager cm = (ConnectivityManager) AppController.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public static String shortUUID() {
        UUID uuid = UUID.randomUUID();
        long l = ByteBuffer.wrap(uuid.toString().getBytes()).getLong();
        return Long.toString(l, Character.MAX_RADIX);
    }
}