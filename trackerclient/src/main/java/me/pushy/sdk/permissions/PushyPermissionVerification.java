package me.pushy.sdk.permissions;

import android.content.Context;
import android.content.pm.PackageManager;

import me.pushy.sdk.config.PushyPermissions;
import me.pushy.sdk.exceptions.PushyException;
import me.pushy.sdk.exceptions.PushyPermissionException;

public class PushyPermissionVerification {
    public static void verifyManifestPermissions(Context context) throws PushyException {
        for (String permission : PushyPermissions.REQUIRED_MANIFEST_PERMISSIONS) {
            if (context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
                continue;
            throw new PushyPermissionException("Error: " + permission + " is missing from your AndroidManifest.xml.");
        }
    }
}

