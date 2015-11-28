package com.em.client.activity.em;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.em.R;
import com.em.client.AppController;
import com.em.client.helper.CallBack;
import com.em.client.utils.EmUtils;
import com.em.client.vo.Route;
import com.em.client.vo.Stop;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import me.pushy.sdk.EmPushy;
import me.pushy.sdk.config.PushySocket;
import me.pushy.sdk.config.TopicType;
import me.pushy.sdk.exceptions.PushyException;
import me.pushy.sdk.services.EmPushySocketService;
import me.pushy.sdk.services.MqttServiceBinder;

public class EmMainActivityClient extends ActionBarActivity implements OnMapReadyCallback, ServiceConnection {
    private GoogleMap googleMap;
    private Marker marker;
    private EmPushySocketService mqttService;
    private boolean bindedService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        MapFragment fm = (MapFragment) getFragmentManager().findFragmentById(R.id.mymap);
        fm.getMapAsync(this);
        if (!EmUtils.isGooglePlayServicesAvailable())
            Toast.makeText(getApplication(), "Google play services are not available.", Toast.LENGTH_SHORT);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        drawRoute();
        try {
            startMqttService();
        } catch (PushyException e) {
            Log.d("Pushy", "Connect exception: " + e.toString());
            Toast.makeText(getApplication(), "Could not connect to server", Toast.LENGTH_SHORT);
        }
    }

    private void drawRoute() {
        Route route = AppController.getInstance().getRoute();
        PolylineOptions polyLineOptions = new PolylineOptions();
        List<LatLng> points = new ArrayList<LatLng>();
        for (com.em.client.vo.LatLng latLng : route.getRoutePoints()) {
            LatLng position = new LatLng(latLng.getLat(), latLng.getLng());
            points.add(position);
        }
        for (Stop stop : route.getRouteStops()) {
            LatLng location = new LatLng(stop.getLatLng().getLat(), stop.getLatLng().getLng());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(location).title(stop.getStopName());
            if (stop.getOrder() == 0) {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.start_marker);
                markerOptions.icon(icon);
            } else if (stop.getOrder() == route.getRouteStops().size() - 1) {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.school_marker);
                markerOptions.icon(icon);
            }
            googleMap.addMarker(markerOptions);
        }
        polyLineOptions.addAll(points);
        polyLineOptions.width(8);
        polyLineOptions.color(Color.BLUE);
        googleMap.addPolyline(polyLineOptions);
    }

    private void updateMap(LatLng location) {
        TextView locationText = (TextView) findViewById(R.id.location);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 15);
        if (googleMap != null) {
            MarkerOptions markerOptions;
            googleMap.animateCamera(cameraUpdate);
            if (marker == null) {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.bus_marker);
                markerOptions = new MarkerOptions();
                markerOptions.position(location).title("Bus");
                markerOptions.icon(icon);
                marker = googleMap.addMarker(markerOptions);
            } else
                marker.setPosition(location);
        }
        locationText.setText("Latitude:" + location.latitude + ", Longitude:" + location.longitude);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void startMqttService() throws PushyException {
        if (this.mqttService == null) {
            EmPushy.startPushy(this, TopicType.NOTIFICATION.toString());
//            Intent serviceStartIntent = EmPushyServiceManager.startSocketService(this);
//            this.bindService(serviceStartIntent, this, Context.BIND_AUTO_CREATE);
        }
    }

    private void publish(String topic, String message) {
        if (bindedService == true) {
            try {
                mqttService.publish(topic, message);
            } catch (Exception e) {
                Toast.makeText(getApplication(), "Failed to publish topic", Toast.LENGTH_SHORT);
            }
        }
    }

    private void subscribe(String topic, int qos, CallBack<Intent> callBack) {
        if (bindedService == true) {
            try {
                mqttService.subscribe(topic, qos, callBack);
            } catch (Exception e) {
                Toast.makeText(getApplication(), "Failed to subscribe topic", Toast.LENGTH_SHORT);
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        EmMainActivityClient.this.mqttService = ((MqttServiceBinder) binder).getService();
        EmMainActivityClient.this.bindedService = true;
        subscribe(TopicType.COORDINATE.toString(), PushySocket.QOS, new CallBack<Intent>() {
            @Override
            public void callBack(Intent intent) {
                updateMap(new LatLng(Double.parseDouble(intent.getStringExtra("lat")), Double.parseDouble(intent.getStringExtra("lng"))));
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        EmMainActivityClient.this.mqttService = null;
    }
}