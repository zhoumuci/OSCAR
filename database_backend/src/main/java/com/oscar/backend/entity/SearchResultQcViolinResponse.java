package com.oscar.backend.entity;

import java.util.List;

public class SearchResultQcViolinResponse {

    private String datasetId;
    private String groupBy;
    private List<SearchResultQcMetricResponse> metrics;

    public SearchResultQcViolinResponse() {
    }

    public SearchResultQcViolinResponse(String datasetId, String groupBy, List<SearchResultQcMetricResponse> metrics) {
        this.datasetId = datasetId;
        this.groupBy = groupBy;
        this.metrics = metrics;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public List<SearchResultQcMetricResponse> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<SearchResultQcMetricResponse> metrics) {
        this.metrics = metrics;
    }
}
