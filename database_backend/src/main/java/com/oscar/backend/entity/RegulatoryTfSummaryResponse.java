package com.oscar.backend.entity;

public class RegulatoryTfSummaryResponse {

    private String datasetId;
    private String domain;
    private String featureType;
    private String featureId;
    private Boolean available;
    private String status;
    private Object tf;
    private String reason;

    public RegulatoryTfSummaryResponse() {
    }

    public RegulatoryTfSummaryResponse(
            String datasetId,
            String domain,
            String featureType,
            String featureId,
            Boolean available,
            String status,
            Object tf,
            String reason
    ) {
        this.datasetId = datasetId;
        this.domain = domain;
        this.featureType = featureType;
        this.featureId = featureId;
        this.available = available;
        this.status = status;
        this.tf = tf;
        this.reason = reason;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getTf() {
        return tf;
    }

    public void setTf(Object tf) {
        this.tf = tf;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
