package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class BedtoolsIntersectResponse {

    private String status;
    private String message;
    private String datasetId;
    private String domain;
    private String genomeBuild;
    private String coordinateSystem = "BED 0-based half-open";
    private BedtoolsQueryRegion queryRegion;
    private List<String> selectedAnnotationTypes = new ArrayList<>();
    private Integer page;
    private Integer pageSize;
    private Long total;
    private BedtoolsIntersectSummary summary;
    private List<BedtoolsOverlapRecord> records = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }

    public String getCoordinateSystem() {
        return coordinateSystem;
    }

    public void setCoordinateSystem(String coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }

    public BedtoolsQueryRegion getQueryRegion() {
        return queryRegion;
    }

    public void setQueryRegion(BedtoolsQueryRegion queryRegion) {
        this.queryRegion = queryRegion;
    }

    public List<String> getSelectedAnnotationTypes() {
        return selectedAnnotationTypes;
    }

    public void setSelectedAnnotationTypes(List<String> selectedAnnotationTypes) {
        this.selectedAnnotationTypes = selectedAnnotationTypes;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public BedtoolsIntersectSummary getSummary() {
        return summary;
    }

    public void setSummary(BedtoolsIntersectSummary summary) {
        this.summary = summary;
    }

    public List<BedtoolsOverlapRecord> getRecords() {
        return records;
    }

    public void setRecords(List<BedtoolsOverlapRecord> records) {
        this.records = records;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
