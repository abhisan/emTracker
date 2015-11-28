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

import com.em.client.helper.CallBack;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import me.pushy.sdk.config.PushyAction;
import me.pushy.sdk.config.PushySocket;
import me.pushy.sdk.config.TopicType;
import me.pushy.sdk.json.PushySingleton;
import me.pushy.sdk.persistence.PushyPersistance;

public class EmPushySocketService extends Service {
    private static MqttPersistence MQTT_PERSISTENCE = null;
    private WifiManager wifi;
    private AlarmManager alarm;
    private ConnectivityManager connectivity;
    private NotificationManager notifications;
    private WifiLock wifiWakeLock;
    private EmPushySocketService.MqttPushConnection connection;
    private long retryInterval = 500L;
    private MqttServiceBinder mqttServiceBinder;
    private BroadcastReceiver connectivityListener = new BroadcastReceiver() {
        public void onReceive(Context context, Intent Intent) {
            boolean internetConnected = EmPushySocketService.this.isNetworkAvailable();
            Log.d("Pushy", "Internet connected: " + internetConnected);
            if (internetConnected) {
                if (EmPushySocketService.this.getConnectedNetwork() == 1 && EmPushySocketService.this.connection.isConnected() && !EmPushySocketService.this.connection.isConnecting() && EmPushySocketService.this.connection.getNetwork() == 0) {
                    Log.d("Pushy", "Reconnecting via wifi");
                    (EmPushySocketService.this.new ConnectAsync()).execute(new Integer[0]);
                    return;
                }
                EmPushySocketService.this.attemptReconnect();
            } else {
                EmPushySocketService.this.cancelReconnect();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Pushy", "Creating service");
        this.wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        this.alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        this.connectivity = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.notifications = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        this.connection = new EmPushySocketService.MqttPushConnection();
        this.handleCrashedService();
        this.mqttServiceBinder = new MqttServiceBinder(this);
        this.start();
        this.registerReceiver(this.connectivityListener, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Pushy", "Service started with intent: " + intent);
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(PushyAction.ACTION_START)) {
                this.start();
            } else if (intent.getAction().equals(PushyAction.ACTION_KEEP_ALIVE)) {
                this.sendKeepAlive();
            } else if (intent.getAction().equals(PushyAction.ACTION_RECONNECT)) {
                this.attemptReconnect();
            } else if (intent.getAction().equals(PushyAction.ACTION_BACKGROUND)) {
                this.stopForeground();
            }
        } else {
            this.start();
        }
        return 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        String activityToken = intent.getStringExtra("MqttService.activityToken");
        this.mqttServiceBinder.setActivityToken(activityToken);
        return this.mqttServiceBinder;
    }

    private void start() {
        if (this.isNetworkAvailable()) {
            if (!this.connection.isConnecting() && !this.connection.isConnected()) {
                (new EmPushySocketService.ConnectAsync()).execute(new Integer[0]);
            } else {
                Log.d("Pushy", "Attempt to start connection that is already active");
            }
        }
    }

    public static Intent performAction(Context context, String action) {
        Intent actionIntent = new Intent(context, EmPushySocketService.class);
        actionIntent.setAction(action);
        context.startService(actionIntent);
        return actionIntent;
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

    public void publish(String topic, String message) throws Exception {
        this.connection.publish(topic, message);
    }

    public void subscribe(String topic, int qos, CallBack<Intent> callBack) throws Exception {
        this.setCallback(callBack);
        int[] qoss = {qos};
        String[] topics = {topic};
        this.connection.subscribe(topics, qoss, callBack);
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
        long Interval = (long) (PushySocket.MQTT_KEEP_ALIVE * 1000);
        PendingIntent pendingKeepAlive = this.getAlarmPendingIntent(PushyAction.ACTION_KEEP_ALIVE);
        this.alarm.setRepeating(0, System.currentTimeMillis() + Interval, Interval, pendingKeepAlive);
        this.acquireWifiLock();
    }

    private PendingIntent getAlarmPendingIntent(String Action) {
        Intent KeepAlive = new Intent();
        KeepAlive.setClass(this, EmPushySocketService.class);
        KeepAlive.setAction(Action);
        return PendingIntent.getService(this, 0, KeepAlive, 0);
    }

    private void stopKeepAliveTimerAndWifilock() {
        PendingIntent PendingKeepAlive = this.getAlarmPendingIntent(PushyAction.ACTION_KEEP_ALIVE);
        this.alarm.cancel(PendingKeepAlive);
        this.releaseWifiLock();
    }

    public void scheduleReconnect() {
        long Now = System.currentTimeMillis();
        if (this.retryInterval >= 60000L) {
            this.retryInterval = 500L;
        }
        this.retryInterval = Math.min(this.retryInterval * 2L, 60000L);
        Log.d("Pushy", "Reconnecting in " + this.retryInterval + "ms.");
        PendingIntent PendingKeepAlive = this.getAlarmPendingIntent(PushyAction.ACTION_RECONNECT);
        this.alarm.set(0, Now + this.retryInterval, PendingKeepAlive);
    }

    public void cancelReconnect() {
        PendingIntent PendingReconnect = this.getAlarmPendingIntent(PushyAction.ACTION_RECONNECT);
        this.alarm.cancel(PendingReconnect);
    }

    private void attemptReconnect() {
        if (this.isNetworkAvailable()) {
            if (!this.connection.isConnected() && !this.connection.isConnecting()) {
                Log.d("Pushy", "Reconnecting...");
                (new EmPushySocketService.ConnectAsync()).execute(new Integer[0]);
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
                this.client = MqttClient.createMqttClient(PushySocket.ENDPOINT + "@" + PushySocket.PORT, EmPushySocketService.MQTT_PERSISTENCE);
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
            this.client = MqttClient.createMqttClient(PushySocket.ENDPOINT + "@" + PushySocket.PORT, EmPushySocketService.MQTT_PERSISTENCE);
            this.client.registerSimpleHandler(this);
            this.network = EmPushySocketService.this.getConnectedNetwork();
            this.client.connect(EmPushySocketService.this.getClientID(), PushySocket.MQTT_CLEAN_START, PushySocket.MQTT_KEEP_ALIVE);
            Log.d("Pushy", "Connected (client ID: " + EmPushySocketService.this.getClientID() + ")");
            EmPushySocketService.this.startKeepAliveTimerAndWifilock();
            EmPushySocketService.this.retryInterval = 500L;
        }

        public void publish(String Topic, String Payload) throws Exception {
            if (this.client != null && this.client.isConnected()) {
                this.client.publish(Topic, Payload.getBytes(), PushySocket.MQTT_QUALITY_OF_SERVICE, PushySocket.MQTT_RETAINED_PUBLISH);
            } else {
                throw new Exception("publish failed: not connected");
            }
        }

        public void subscribe(String[] topic, int[] qos) throws Exception {
            if (this.client != null && this.client.isConnected()) {
                this.client.subscribe(topic, qos);
            } else {
                throw new Exception("publish failed: not connected");
            }
        }

        public void subscribe(String[] topic, int[] qos, CallBack<Intent> callBack) throws Exception {
            if (this.client != null && this.client.isConnected()) {
                EmPushySocketService.this.callback = callBack;
                this.client.subscribe(topic, qos);
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
            String Locale = EmPushySocketService.this.getResources().getConfiguration().locale.getLanguage();
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

        @Override
        public void connectionLost() throws Exception {
            Log.d("Pushy", "connection lost");
            EmPushySocketService.this.stopKeepAliveTimerAndWifilock();
            EmPushySocketService.this.attemptReconnect();
        }

        private void parsePayload(byte[] payload, Intent intent) {
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
            } catch (Exception e) {
                Log.e("Pushy", e.getMessage(), e);
            }
        }

        @Override
        public void publishArrived(String topic, byte[] payload, int qos, boolean retained) {
            Log.d("Pushy", "Received push for package " + topic);
            Intent push = new Intent();
            this.parsePayload(payload, push);
            push.setPackage("com.em");
            push.setAction("pushy.me");
            EmPushySocketService.this.sendBroadcast(push);
            if (callback != null) {
                callback.callBack(push);
            }
        }
    }

    public void setCallback(CallBack<Intent> callback) {
        this.callback = callback;
    }

    public CallBack<Intent> callback;

    public class ConnectAsync extends AsyncTask<Integer, String, Integer> {
        public ConnectAsync() {
            EmPushySocketService.this.connection.setConnecting(true);
        }

        protected Integer doInBackground(Integer... Parameter) {
            Log.d("Pushy", "connecting...");
            try {
                EmPushySocketService.this.connection.connect();
                String[] topics = {TopicType.NOTIFICATION.toString()};
                int[] qoS = {PushySocket.QOS};
                EmPushySocketService.this.connection.subscribe(topics, qoS);
            } catch (Exception var3) {
                Log.d("Pushy", "Connect exception: " + var3.toString());
                if (EmPushySocketService.this.isNetworkAvailable()) {
                    EmPushySocketService.this.scheduleReconnect();
                }
            }
            EmPushySocketService.this.connection.setConnecting(false);
            return Integer.valueOf(0);
        }
    }
}