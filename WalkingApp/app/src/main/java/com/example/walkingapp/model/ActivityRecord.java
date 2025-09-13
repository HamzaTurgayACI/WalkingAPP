package com.example.walkingapp.model;

import org.osmdroid.util.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActivityRecord implements Serializable {
    private String elapsedTime;
    private double distance;
    private int calories;
    private List<Map<String, Double>> routePoints; // Firestore için uygun format
    private long timestamp; // Kaydedilen zaman
    private String userId;

    // Boş Constructor (Firebase için gerekli)
    public ActivityRecord() {
    }

    // Parametreli Constructor
    public ActivityRecord(String elapsedTime, double distance, int calories, List<Map<String, Double>> routePoints, long timestamp,String userId) {
        this.elapsedTime = elapsedTime;
        this.distance = distance;
        this.calories = calories;
        this.routePoints = routePoints;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    // Getter Metotları
    public String getElapsedTime() {
        return elapsedTime;
    }

    public double getDistance() {
        return distance;
    }

    public int getCalories() {
        return calories;
    }

    public List<Map<String, Double>> getRoutePoints() {
        return routePoints;
    }

    public long getTimestamp() {
        return timestamp;
    }

    //  Yeni: setRoutePoints Metodu
    public void setRoutePoints(List<Map<String, Double>> routePoints) {
        this.routePoints = routePoints;
    }

    //  GeoPoint Listesine Dönüştürme Metodu
    public List<GeoPoint> getGeoPoints() {
        List<GeoPoint> geoPoints = new ArrayList<>();
        if (routePoints != null) {
            for (Map<String, Double> point : routePoints) {
                if (point.containsKey("latitude") && point.containsKey("longitude")) {
                    double lat = point.get("latitude");
                    double lon = point.get("longitude");
                    geoPoints.add(new GeoPoint(lat, lon));
                }
            }
        }
        return geoPoints;
    }
}
