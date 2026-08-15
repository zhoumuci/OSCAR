package com.oscar.backend.entity;

/**
 * Lightweight DTO for the frontend visitor map.
 */
public class VisitorPoint {

    private String name;
    private double lat;
    private double lon;
    private long value;

    public VisitorPoint() {}

    public VisitorPoint(String name, double lat, double lon, long value) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }
}
