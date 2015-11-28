package me.pushy.sdk.config;

import android.os.Environment;

public class PushyStorage {
    public static final String EXTERNAL_STORAGE_FILE = "registration.id";
    public static final String EXTERNAL_STORAGE_DIRECTORY = Environment.getExternalStorageDirectory() + "/Android/data/me.pushy.sdk/";
}

