/*
 * Decompiled with CFR 0_102.
 */
package me.pushy.sdk.format;

public class PushyStringUtils {
    public static boolean stringIsNullOrEmpty(String input) {
        if (input == null) {
            return true;
        }
        if (input.trim().equals("")) {
            return true;
        }
        return false;
    }
}

