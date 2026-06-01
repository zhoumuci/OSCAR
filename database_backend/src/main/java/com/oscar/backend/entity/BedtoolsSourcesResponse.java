package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class BedtoolsSourcesResponse {

    private String datasetId;
    private String domain;
    private String genomeBuild;
    private String coordinateSystem = "BED 0-based half-open";
    private List<BedtoolsSourceOption> sources = new ArrayList<>();

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

    public List<BedtoolsSourceOption> getSources() {
        return sources;
    }

    public void setSources(List<BedtoolsSourceOption> sources) {
        this.sources = sources;
    }
}
