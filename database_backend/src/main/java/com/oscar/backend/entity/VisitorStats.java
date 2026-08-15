package com.oscar.backend.entity;

/**
 * Aggregated visitor statistics for the home page dashboard.
 */
public class VisitorStats {

    private long totalVisitors;
    private long countryCount;
    private long activeToday;

    public VisitorStats() {}

    public VisitorStats(long totalVisitors, long countryCount, long activeToday) {
        this.totalVisitors = totalVisitors;
        this.countryCount = countryCount;
        this.activeToday = activeToday;
    }

    public long getTotalVisitors() { return totalVisitors; }
    public void setTotalVisitors(long totalVisitors) { this.totalVisitors = totalVisitors; }

    public long getCountryCount() { return countryCount; }
    public void setCountryCount(long countryCount) { this.countryCount = countryCount; }

    public long getActiveToday() { return activeToday; }
    public void setActiveToday(long activeToday) { this.activeToday = activeToday; }
}
