package sg.edu.nyp.alexia.model;

import com.mapbox.mapboxsdk.annotations.Marker;

/**
 * Created by Jeffry on 11/1/17.
 */

public class ATM {
    private String name;
    private double lat;
    private double lng;
    private double distance;
    private Marker marker;

    public ATM(){

    }

    public ATM(String name, double lat, double lng, double distance, Marker marker) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.distance = distance;
        this.marker = marker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }
}
