package com.oscar.backend.entity;

import java.util.List;

public class SearchResultQcMetricResponse {

    private String metric;
    private String label;
    private List<SearchResultQcGroupResponse> groups;

    public SearchResultQcMetricResponse() {
    }

    public SearchResultQcMetricResponse(String metric, String label, List<SearchResultQcGroupResponse> groups) {
        this.metric = metric;
        this.label = label;
        this.groups = groups;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<SearchResultQcGroupResponse> getGroups() {
        return groups;
    }

    public void setGroups(List<SearchResultQcGroupResponse> groups) {
        this.groups = groups;
    }
}
