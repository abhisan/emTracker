package com.em.embustracker.activity;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.em.embustracker.R;
import com.em.embustracker.utils.EmUtils;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivityClient extends FragmentActivity {
    private Socket mSocket;

    GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_map);
        super.onCreate(savedInstanceState);
        final String url = EmUtils.url();
        final String route = EmUtils.route();
        final String client = EmUtils.client();
        final Activity _this = this;
        if (url.isEmpty()) {
            Toast.makeText(getApplication(), "Could not connect to server.", Toast.LENGTH_SHORT);
            return;
        }
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            Toast.makeText(getApplication(), "Could not connect to server.", Toast.LENGTH_SHORT);
        }
        mSocket.on("ack", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("trackerId", route);
                    obj.put("clientId", client);
                    mSocket.emit("subscribe_location", obj);
                } catch (Exception e) {
                    //TODO: Exception handling
                }
            }
        });

        mSocket.on("location_updated", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                _this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        Double longitude;
                        Double latitude;
                        try {
                            longitude = data.getDouble("longitude");
                            latitude = data.getDouble("latitude");
                            updateMap(longitude, latitude);
                        } catch (JSONException e) {
                            //TODO: Exception handling
                            return;
                        }
                    }
                });

            }
        });
        mSocket.connect();
        MapFragment fm = (MapFragment) getFragmentManager().findFragmentById(R.id.mymap);
        googleMap = fm.getMap();
        googleMap.setMyLocationEnabled(true);
    }

    private void updateMap(Double longitude, Double latitude) {
        TextView tvLocation = (TextView) findViewById(R.id.tv_location);
        LatLng latLng = new LatLng(latitude, longitude);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
        //googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        tvLocation.setText("Latitude:" + latitude + ", Longitude:" + longitude);
    }

    private void updateServer(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        JSONObject obj = new JSONObject();
        try {
            final String route = EmUtils.route();
            obj.put("trackerId", route);
            obj.put("longitude", latitude);
            obj.put("trackerId", longitude);
            mSocket.emit("update_location", obj);
        } catch (Exception e) {
            //TODO: Exception handling
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        //mSocket.off("new message", onNewMessage);
    }
}