package com.em.tracker.activity;

import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.em.tracker.AppController;
import com.em.tracker.R;
import com.em.tracker.vo.Route;
import com.em.tracker.vo.Stop;
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

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class MainActivityTracker extends ActionBarActivity implements LocationListener, OnMapReadyCallback {
    private GoogleMap googleMap;
    private Socket socketIOClient;
    private LocationManager locationManager;
    private String provider;
    private Marker marker;
    private MapFragment fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        createSocketIOClient();
        initilizeMap();
        if (!com.em.tracker.utils.EmUtils.isGooglePlayServicesAvailable())
            Toast.makeText(getApplication(), "Google play services are not available.", Toast.LENGTH_SHORT);
        //googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private void initilizeMap() {
        if (fm == null) {
            fm = ((MapFragment) getFragmentManager().findFragmentById(R.id.mymap));
            fm.getMapAsync(this);
        }
    }

    private void createSocketIOClient() {
        try {
            socketIOClient = IO.socket(com.em.tracker.utils.EmUtils.getIOServerSocketUrl());
        } catch (URISyntaxException e) {
            Toast.makeText(getApplication(), "Could not connect to server.", Toast.LENGTH_SHORT);
        }
        socketIOClient.on("ack", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("routeId", AppController.getInstance().getRoute().getVehicleRouteId());
                    socketIOClient.emit("register_tracker", obj);
                } catch (Exception e) {
                    //TODO: Exception handling
                }
            }
        });
        socketIOClient.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        Criteria criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(getApplication(), "GPS is disabled.", Toast.LENGTH_SHORT);
        }
        provider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(provider, 500, 20, this);

        Route route = AppController.getInstance().getRoute();
        PolylineOptions polyLineOptions = new PolylineOptions();
        List<LatLng> points = new ArrayList<LatLng>();
        for (com.em.tracker.vo.LatLng latLng : route.getRoutePoints()) {
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

    @Override
    public void onLocationChanged(Location location) {
        updateMap(location);
        updateServer(location);
    }

    private void updateMap(Location location) {
        TextView locationText = (TextView) findViewById(R.id.location);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
        MarkerOptions markerOptions;
        if (marker == null) {
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.bus_marker);
            markerOptions = new MarkerOptions();
            markerOptions.position(latLng).title("Bus");
            markerOptions.icon(icon);
            marker = googleMap.addMarker(markerOptions);
        }
        marker.setPosition(latLng);
        locationText.setText("Latitude:" + latitude + ", Longitude:" + longitude);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
        if (locationManager != null)
            locationManager.requestLocationUpdates(provider, 500, 20, this);
    }

    @Override
    public void onProvideAssistData(Bundle data) {
        super.onProvideAssistData(data);
    }

    private void updateServer(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        JSONObject obj = new JSONObject();
        try {
            obj.put("routeId", AppController.getInstance().getRoute().getVehicleRouteId());
            obj.put("lat", latitude);
            obj.put("lng", longitude);
            socketIOClient.emit("update_location", obj);
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
        socketIOClient.disconnect();
        //socketIOClient.off("new message", onNewMessage);
    }
}