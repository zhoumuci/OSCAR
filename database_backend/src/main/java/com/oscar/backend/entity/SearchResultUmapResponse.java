package com.oscar.backend.entity;

import java.util.List;

public class SearchResultUmapResponse {

    private String datasetId;
    private String domain;
    private String embedding;
    private String colorBy;
    private Long total;
    private Integer returned;
    private List<SearchResultUmapPointResponse> points;

    public SearchResultUmapResponse() {
    }

    public SearchResultUmapResponse(
            String datasetId,
            String colorBy,
            Long total,
            Integer returned,
            List<SearchResultUmapPointResponse> points
    ) {
        this.datasetId = datasetId;
        this.colorBy = colorBy;
        this.total = total;
        this.returned = returned;
        this.points = points;
    }

    public SearchResultUmapResponse(
            String datasetId,
            String domain,
            String embedding,
            String colorBy,
            Long total,
            Integer returned,
            List<SearchResultUmapPointResponse> points
    ) {
        this.datasetId = datasetId;
        this.domain = domain;
        this.embedding = embedding;
        this.colorBy = colorBy;
        this.total = total;
        this.returned = returned;
        this.points = points;
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

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public String getColorBy() {
        return colorBy;
    }

    public void setColorBy(String colorBy) {
        this.colorBy = colorBy;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getReturned() {
        return returned;
    }

    public void setReturned(Integer returned) {
        this.returned = returned;
    }

    public List<SearchResultUmapPointResponse> getPoints() {
        return points;
    }

    public void setPoints(List<SearchResultUmapPointResponse> points) {
        this.points = points;
    }
}
