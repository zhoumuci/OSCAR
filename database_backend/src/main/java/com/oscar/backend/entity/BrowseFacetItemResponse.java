package com.oscar.backend.entity;

public class BrowseFacetItemResponse {

    private String label;
    private Long count;

    public BrowseFacetItemResponse() {
    }

    public BrowseFacetItemResponse(String label, Long count) {
        this.label = label;
        this.count = count;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
