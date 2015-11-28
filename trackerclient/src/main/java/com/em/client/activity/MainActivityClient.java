package com.em.client.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.em.R;
import com.em.client.AppController;
import com.em.client.utils.EmUtils;
import com.em.client.vo.Route;
import com.em.client.vo.Stop;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MainActivityClient extends ActionBarActivity implements OnMapReadyCallback {
    private Socket mSocket;
    private GoogleMap googleMap;
    private Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        try {
            mSocket = IO.socket(EmUtils.getIOServerSocketUrl());
        } catch (URISyntaxException e) {
            Toast.makeText(getApplication(), "Could not connect to server.", Toast.LENGTH_SHORT);
        }
        mSocket.on("ack", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("routeId", AppController.getInstance().getRoute().getVehicleRouteId());
                    mSocket.emit("subscribe_location", obj);
                } catch (Exception e) {
                    //TODO: Exception handling
                }
            }
        });
        mSocket.on("location_updated", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                MainActivityClient.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            updateMap(new LatLng(data.getDouble("lat"), data.getDouble("lng")));
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
        fm.getMapAsync(this);
        if (!EmUtils.isGooglePlayServicesAvailable())
            Toast.makeText(getApplication(), "Google play services are not available.", Toast.LENGTH_SHORT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.menu_bus_details) {
//            return true;
//        } else if (id == R.id.menu_driver_details) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        //map.setMyLocationEnabled(true);
        googleMap = map;
        Route route = AppController.getInstance().getRoute();
        PolylineOptions polyLineOptions = new PolylineOptions();
        List<LatLng> points = new ArrayList<LatLng>();
        for (com.em.client.vo.LatLng latLng : route.getRoutePoints()) {
            LatLng position = new LatLng(latLng.getLat(), latLng.getLng());
            points.add(position);
        }
        LatLng startPoint = null;
        for (Stop stop : route.getRouteStops()) {
            LatLng location = new LatLng(stop.getLatLng().getLat(), stop.getLatLng().getLng());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(location).title(stop.getStopName());
            if (stop.getOrder() == 1) {
                startPoint = new LatLng(stop.getLatLng().getLat(), stop.getLatLng().getLng());
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.start_marker);
                markerOptions.icon(icon);
            } else if (stop.getOrder() == route.getRouteStops().size()) {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.school_marker);
                markerOptions.icon(icon);
            }
            googleMap.addMarker(markerOptions);
        }
        polyLineOptions.addAll(points);
        polyLineOptions.width(8);
        polyLineOptions.color(Color.BLUE);
        googleMap.addPolyline(polyLineOptions);
        if (startPoint != null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(startPoint, 15);
            googleMap.animateCamera(cameraUpdate);
        }
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
        //googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        locationText.setText("Latitude:" + location.latitude + ", Longitude:" + location.longitude);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        //mSocket.off("new message", onNewMessage);
    }
}