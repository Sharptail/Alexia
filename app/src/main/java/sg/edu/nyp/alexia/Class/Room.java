package sg.edu.nyp.alexia.Class;

/**
 * Created by Jeffry on 5/1/17.
 */

public class Room {
    private String name;
    private int level;
    private double lat;
    private double lng;

    public Room(){

    }

    public Room(String name, int level, double lat, double lng) {
        this.name = name;
        this.level = level;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
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
}
