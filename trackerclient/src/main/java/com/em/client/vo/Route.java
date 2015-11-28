package com.em.client.vo;

import java.util.List;

public final class Route {
    private String vehicleRouteId;
    private String vehicleRouteName;
    private List<Stop> routeStops;
    private List<LatLng> routePoints;

    public List<LatLng> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<LatLng> routePoints) {
        this.routePoints = routePoints;
    }

    public String getVehicleRouteId() {
        return vehicleRouteId;
    }

    public void setVehicleRouteId(String vehicleRouteId) {
        this.vehicleRouteId = vehicleRouteId;
    }

    public String getVehicleRouteName() {
        return vehicleRouteName;
    }

    public void setVehicleRouteName(String vehicleRouteName) {
        this.vehicleRouteName = vehicleRouteName;
    }

    public List<Stop> getRouteStops() {
        return routeStops;
    }

    public void setRouteStops(List<Stop> routeStops) {
        this.routeStops = routeStops;
    }
}