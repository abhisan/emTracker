package com.em.embustracker.activity;

import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.em.embustracker.R;
import com.em.embustracker.utils.EmUtils;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivityTracker extends FragmentActivity implements LocationListener {
    private Socket mSocket;
    GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_map);
        super.onCreate(savedInstanceState);
        final String url = EmUtils.url();
        final String route = EmUtils.route();
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
                    mSocket.emit("register_tracker", obj);
                } catch (Exception e) {
                    //TODO: Exception handling
                }
            }
        });
        mSocket.connect();
        MapFragment fm = (MapFragment) getFragmentManager().findFragmentById(R.id.mymap);
        googleMap = fm.getMap();
        googleMap.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            onLocationChanged(location);
        }
        //googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        locationManager.requestLocationUpdates(provider, 2000, 0, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        updateMap(location);
        updateServer(location);
    }

    private void updateMap(Location location) {
        TextView tvLocation = (TextView) findViewById(R.id.tv_location);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
        tvLocation.setText("Latitude:" + latitude + ", Longitude:" + longitude);
    }

    private void updateServer(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        JSONObject obj = new JSONObject();
        try {
            final String route = EmUtils.route();
            obj.put("trackerId", route);
            obj.put("longitude", longitude);
            obj.put("latitude", latitude);
            mSocket.emit("update_location", obj);
        } catch (Exception e) {
            //TODO: Exception handling
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        //mSocket.off("new message", onNewMessage);
    }
}