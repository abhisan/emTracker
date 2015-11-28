//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package me.pushy.sdk.services;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.em.client.utils.EmUtils;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import me.pushy.sdk.config.PushySocket;
import me.pushy.sdk.json.PushySingleton;
import me.pushy.sdk.persistence.PushyPersistance;

public class PushySocketService extends Service {
    public static final String ACTION_START = "Pushy.START";
    public static final String ACTION_RECONNECT = "Pushy.RECONNECT";
    public static final String ACTION_KEEP_ALIVE = "Pushy.KEEP_ALIVE";
    public static final String ACTION_FOREGROUND = "Pushy.FOREGROUND";
    public static final String ACTION_BACKGROUND = "Pushy.BACKGROUND";
    private static final long INITIAL_RETRY_INTERVAL = 500L;
    private static final long MAXIMUM_RETRY_INTERVAL = 60000L;
    private static MqttPersistence MQTT_PERSISTENCE = null;
    private static boolean MQTT_CLEAN_START = true;
    private static short MQTT_KEEP_ALIVE = 301;
    private static int MQTT_QUALITY_OF_SERVICE = 1;
    private static boolean MQTT_RETAINED_PUBLISH = false;
    private WifiManager wifi;
    private AlarmManager alarm;
    private ConnectivityManager connectivity;
    private NotificationManager notifications;
    private WifiLock wifiWakeLock;
    private PushySocketService.MqttPushConnection connection;
    private long RetryInterval = 500L;
    private BroadcastReceiver connectivityListener = new BroadcastReceiver() {
        public void onReceive(Context context, Intent Intent) {
            boolean internetConnected = PushySocketService.this.isNetworkAvailable();
            Log.d("Pushy", "Internet connected: " + internetConnected);
            if (internetConnected) {
                if (PushySocketService.this.getConnectedNetwork() == 1 && PushySocketService.this.connection.isConnected() && !PushySocketService.this.connection.isConnecting() && PushySocketService.this.connection.getNetwork() == 0) {
                    Log.d("Pushy", "Reconnecting via wifi");
                    (PushySocketService.this.new ConnectAsync()).execute(new Integer[0]);
                    return;
                }
                PushySocketService.this.attemptReconnect();
            } else {
                PushySocketService.this.cancelReconnect();
            }
        }
    };

    public static void performAction(Context context, String action) {
        Intent actionIntent = new Intent(context, PushySocketService.class);
        actionIntent.setAction(action);
        context.startService(actionIntent);
    }

    public void onCreate() {
        super.onCreate();
        Log.d("Pushy", "Creating service");
        this.wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        this.alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        this.connectivity = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.notifications = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        this.connection = new PushySocketService.MqttPushConnection();
        this.handleCrashedService();
        this.start();
        this.registerReceiver(this.connectivityListener, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.d("Pushy", "Task removed");
        super.onTaskRemoved(rootIntent);
    }

    private void handleCrashedService() {
        this.stopKeepAliveTimerAndWifilock();
        this.cancelReconnect();
    }

    public void onDestroy() {
        Log.d("Pushy", "Service destroyed");
        this.stop();
        super.onDestroy();
    }

    public int onStartCommand(Intent Intent, int flags, int startId) {
        Log.d("Pushy", "Service started with intent: " + Intent);
        if (Intent != null && Intent.getAction() != null) {
            if (Intent.getAction().equals("Pushy.START")) {
                this.start();
            } else if (Intent.getAction().equals("Pushy.KEEP_ALIVE")) {
                this.sendKeepAlive();
            } else if (Intent.getAction().equals("Pushy.RECONNECT")) {
                this.attemptReconnect();
            } else if (Intent.getAction().equals("Pushy.BACKGROUND")) {
                this.stopForeground();
            }
        } else {
            this.start();
        }
        return 1;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start() {
        if (this.isNetworkAvailable()) {
            if (!this.connection.isConnecting() && !this.connection.isConnected()) {
                (new PushySocketService.ConnectAsync()).execute(new Integer[0]);
            } else {
                Log.d("Pushy", "Attempt to start connection that is already active");
            }
        }
    }

    void stopForeground() {
        this.stopForeground(true);
    }

    private void stop() {
        this.stopForeground();
        this.cancelReconnect();
        this.stopKeepAliveTimerAndWifilock();
        this.unregisterReceiver(this.connectivityListener);
    }

    private String getClientID() {
        return PushyPersistance.getToken(this);
    }

    private String getTopic() {
        return PushyPersistance.getTopic(this);
    }

    private void sendKeepAlive() {
        if (this.connection.isConnected()) {
            try {
                this.connection.sendKeepAlive();
            } catch (Exception var2) {
                Log.d("Pushy", "Keep alive error: " + var2.toString(), var2);
            }
        }

    }

    private void startKeepAliveTimerAndWifilock() {
        long Interval = (long) (MQTT_KEEP_ALIVE * 1000);
        PendingIntent pendingKeepAlive = this.getAlarmPendingIntent("Pushy.KEEP_ALIVE");
        this.alarm.setRepeating(0, System.currentTimeMillis() + Interval, Interval, pendingKeepAlive);
        this.acquireWifiLock();
    }

    private PendingIntent getAlarmPendingIntent(String Action) {
        Intent KeepAlive = new Intent();
        KeepAlive.setClass(this, PushySocketService.class);
        KeepAlive.setAction(Action);
        return PendingIntent.getService(this, 0, KeepAlive, 0);
    }

    private void stopKeepAliveTimerAndWifilock() {
        PendingIntent PendingKeepAlive = this.getAlarmPendingIntent("Pushy.KEEP_ALIVE");
        this.alarm.cancel(PendingKeepAlive);
        this.releaseWifiLock();
    }

    public void scheduleReconnect() {
        long Now = System.currentTimeMillis();
        if (this.RetryInterval >= 60000L) {
            this.RetryInterval = 500L;
        }
        this.RetryInterval = Math.min(this.RetryInterval * 2L, 60000L);
        Log.d("Pushy", "Reconnecting in " + this.RetryInterval + "ms.");
        PendingIntent PendingKeepAlive = this.getAlarmPendingIntent("Pushy.RECONNECT");
        this.alarm.set(0, Now + this.RetryInterval, PendingKeepAlive);
    }

    public void cancelReconnect() {
        PendingIntent PendingReconnect = this.getAlarmPendingIntent("Pushy.RECONNECT");
        this.alarm.cancel(PendingReconnect);
    }

    private void attemptReconnect() {
        if (this.isNetworkAvailable()) {
            if (!this.connection.isConnected() && !this.connection.isConnecting()) {
                Log.d("Pushy", "Reconnecting...");
                (new PushySocketService.ConnectAsync()).execute(new Integer[0]);
            }
        }
    }

    private void releaseWifiLock() {
        if (this.wifiWakeLock != null) {
            try {
                this.wifiWakeLock.release();
            } catch (Exception var2) {
                Log.d("Pushy", "Wifilock release failed");
            }
            this.wifiWakeLock = null;
            Log.d("Pushy", "WifiLock released");
        }
    }

    private void acquireWifiLock() {
        if (this.wifiWakeLock == null) {
            this.wifiWakeLock = this.wifi.createWifiLock(1, "Pushy");
            this.wifiWakeLock.acquire();
            Log.d("Pushy", "WifiLock acquired");
        }
    }

    private boolean isNetworkAvailable() {
        NetworkInfo ActiveNetwork = this.connectivity.getActiveNetworkInfo();
        return ActiveNetwork != null && ActiveNetwork.isAvailable() && ActiveNetwork.isConnected();
    }

    private int getConnectedNetwork() {
        NetworkInfo wifi = this.connectivity.getNetworkInfo(1);
        NetworkInfo mobile = this.connectivity.getNetworkInfo(0);
        if (wifi != null && wifi.isConnected())
            return 1;
        else {
            return mobile != null && mobile.isConnected() ? 0 : -1;
        }
    }

    private class MqttPushConnection implements MqttSimpleCallback {
        boolean connecting;
        int network;
        IMqttClient client;

        public MqttPushConnection() {
            try {
                this.client = MqttClient.createMqttClient(EmUtils.getMQTTServerUrl(), PushySocketService.MQTT_PERSISTENCE);
            } catch (Exception var3) {
                Log.d("Pushy", "connection initialization error: " + var3.toString());
            }
        }

        public void disconnectExistingClient() {
            if (this.client != null && this.client.isConnected()) {
                try {
                    this.client.disconnect();
                } catch (MqttPersistenceException var2) {
                }
            }
        }

        public void connect() throws Exception {
            this.disconnectExistingClient();
            this.client = MqttClient.createMqttClient(EmUtils.getMQTTServerUrl(), PushySocketService.MQTT_PERSISTENCE);
            this.client.registerSimpleHandler(this);
            this.network = PushySocketService.this.getConnectedNetwork();
            this.client.connect(PushySocketService.this.getClientID(), PushySocketService.MQTT_CLEAN_START, PushySocketService.MQTT_KEEP_ALIVE);
            String[] topics = {getTopic()};
            int[] QoS = {PushySocket.QOS};
            this.client.subscribe(topics, QoS);
            Log.d("Pushy", "Connected (client ID: " + PushySocketService.this.getClientID() + ")");
            PushySocketService.this.startKeepAliveTimerAndWifilock();
            PushySocketService.this.RetryInterval = 500L;
        }

        public void publish(String Topic, String Payload) throws Exception {
            if (this.client != null && this.client.isConnected()) {
                this.client.publish(Topic, Payload.getBytes(), PushySocketService.MQTT_QUALITY_OF_SERVICE, PushySocketService.MQTT_RETAINED_PUBLISH);
            } else {
                throw new Exception("publish failed: not connected");
            }
        }

        public void sendKeepAlive() throws Exception {
            Log.d("Pushy", "Sending keep alive");
            this.publish("keepalive", "");
        }

        public void requestSelfTest() throws Exception {
            Log.d("Pushy", "Requesting self-test");
            String Locale = PushySocketService.this.getResources().getConfiguration().locale.getLanguage();
            this.publish("test", Locale);
        }

        public boolean isConnected() {
            return this.client.isConnected();
        }

        public int getNetwork() {
            return this.network;
        }

        public boolean isConnecting() {
            return this.connecting;
        }

        public void setConnecting(boolean value) {
            this.connecting = value;
        }

        public void connectionLost() throws Exception {
            Log.d("Pushy", "connection lost");
            PushySocketService.this.stopKeepAliveTimerAndWifilock();
            PushySocketService.this.attemptReconnect();
        }

        void parsePayload(byte[] payload, Intent intent) {
            try {
                String exc = new String(payload);
                Map map = (Map) PushySingleton.getJackson().readValue(exc, Map.class);
                Iterator var5 = map.entrySet().iterator();
                while (var5.hasNext()) {
                    Entry entry = (Entry) var5.next();
                    if (entry.getValue() != null) {
                        if (entry.getValue().getClass() == String.class) {
                            intent.putExtra((String) entry.getKey(), (String) entry.getValue());
                        }
                        if (entry.getValue().getClass() == Boolean.class) {
                            intent.putExtra((String) entry.getKey(), (Boolean) entry.getValue());
                        }
                        if (entry.getValue().getClass() == Integer.class) {
                            intent.putExtra((String) entry.getKey(), (Integer) entry.getValue());
                        }
                        if (entry.getValue().getClass() == Long.class) {
                            intent.putExtra((String) entry.getKey(), (Long) entry.getValue());
                        }
                        if (entry.getValue().getClass() == ArrayList.class) {
                            intent.putExtra((String) entry.getKey(), ((ArrayList) entry.getValue()).toArray());
                        }
                    }
                }
            } catch (Exception var7) {
                Log.e("Pushy", var7.getMessage(), var7);
            }
        }

        public void publishArrived(String topic, byte[] payload, int qos, boolean retained) {
            Log.d("Pushy", "Received push for package " + topic);
            Intent push = new Intent();
            this.parsePayload(payload, push);
            push.setPackage("com.em");
            push.setAction("pushy.me");
            PushySocketService.this.sendBroadcast(push);
        }
    }

    public class ConnectAsync extends AsyncTask<Integer, String, Integer> {
        public ConnectAsync() {
            PushySocketService.this.connection.setConnecting(true);
        }

        protected Integer doInBackground(Integer... Parameter) {
            Log.d("Pushy", "connecting...");
            try {
                PushySocketService.this.connection.connect();
            } catch (Exception var3) {
                Log.d("Pushy", "Connect exception: " + var3.toString());
                if (PushySocketService.this.isNetworkAvailable()) {
                    PushySocketService.this.scheduleReconnect();
                }
            }
            PushySocketService.this.connection.setConnecting(false);
            return Integer.valueOf(0);
        }
    }
}