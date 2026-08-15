package com.oscar.backend.entity;

import java.time.LocalDateTime;

public class VisitorLog {

    private Long id;
    private String ipAddress;
    private String country;
    private String city;
    private Double lat;
    private Double lon;
    private Long visitCount;
    private LocalDateTime firstVisit;
    private LocalDateTime lastVisit;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }

    public Long getVisitCount() { return visitCount; }
    public void setVisitCount(Long visitCount) { this.visitCount = visitCount; }

    public LocalDateTime getFirstVisit() { return firstVisit; }
    public void setFirstVisit(LocalDateTime firstVisit) { this.firstVisit = firstVisit; }

    public LocalDateTime getLastVisit() { return lastVisit; }
    public void setLastVisit(LocalDateTime lastVisit) { this.lastVisit = lastVisit; }
}
