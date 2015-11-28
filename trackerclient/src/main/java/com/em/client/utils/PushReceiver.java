package com.em.client.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.em.client.AppController;
import com.em.client.activity.MainActivityClient;

import java.util.Random;

public class PushReceiver extends BroadcastReceiver {
    public static final String notificationTitle = "EM Notification";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getStringExtra("NOTIFICATION") != null) {
            sendNotification(context, intent.getStringExtra("NOTIFICATION"));
        }
    }

    private void sendNotification(Context contex, String message) {
        NotificationManager mNotificationManager = (NotificationManager) contex.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(AppController.getInstance(), MainActivityClient.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                AppController.getInstance(),
                0,
                intent,
                0
        );
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                AppController.getInstance()).setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationTitle))
                .setContentText(message);
        //mBuilder.setContentIntent(contentIntent);
        Notification notification = mBuilder.build();
        notification.defaults = Notification.DEFAULT_ALL;
        notification.when = System.currentTimeMillis();
        Random rand = new Random();
        mNotificationManager.notify(rand.nextInt(), notification);
    }
}