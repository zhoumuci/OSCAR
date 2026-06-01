package com.oscar.backend.entity;

public class RegulatoryNetworkSummaryRow {

    private String nodeKey;
    private Long linkedCount;
    private Double maxLinkScore;
    private Double minCorrelation;
    private Double maxCorrelation;
    private Double minFdr;
    private Double maxFdr;

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public Long getLinkedCount() {
        return linkedCount;
    }

    public void setLinkedCount(Long linkedCount) {
        this.linkedCount = linkedCount;
    }

    public Double getMaxLinkScore() {
        return maxLinkScore;
    }

    public void setMaxLinkScore(Double maxLinkScore) {
        this.maxLinkScore = maxLinkScore;
    }

    public Double getMinCorrelation() {
        return minCorrelation;
    }

    public void setMinCorrelation(Double minCorrelation) {
        this.minCorrelation = minCorrelation;
    }

    public Double getMaxCorrelation() {
        return maxCorrelation;
    }

    public void setMaxCorrelation(Double maxCorrelation) {
        this.maxCorrelation = maxCorrelation;
    }

    public Double getMinFdr() {
        return minFdr;
    }

    public void setMinFdr(Double minFdr) {
        this.minFdr = minFdr;
    }

    public Double getMaxFdr() {
        return maxFdr;
    }

    public void setMaxFdr(Double maxFdr) {
        this.maxFdr = maxFdr;
    }
}
