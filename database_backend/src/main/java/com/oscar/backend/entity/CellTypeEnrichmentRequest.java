package com.oscar.backend.entity;

import java.util.List;

public class CellTypeEnrichmentRequest {

    private List<String> geneSymbols;
    private String tissue;
    private String datasetId;
    private String markerReference = "integration_expression";
    private String resultLevel = "cell_type";
    private Integer minOverlap = 1;
    private String backgroundUniverse = "selected_marker_reference";
    private String fdrMethod = "BH";

    public List<String> getGeneSymbols() {
        return geneSymbols;
    }

    public void setGeneSymbols(List<String> geneSymbols) {
        this.geneSymbols = geneSymbols;
    }

    public String getTissue() {
        return tissue;
    }

    public void setTissue(String tissue) {
        this.tissue = tissue;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getMarkerReference() {
        return markerReference;
    }

    public void setMarkerReference(String markerReference) {
        this.markerReference = markerReference;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public void setResultLevel(String resultLevel) {
        this.resultLevel = resultLevel;
    }

    public Integer getMinOverlap() {
        return minOverlap;
    }

    public void setMinOverlap(Integer minOverlap) {
        this.minOverlap = minOverlap;
    }

    public String getBackgroundUniverse() {
        return backgroundUniverse;
    }

    public void setBackgroundUniverse(String backgroundUniverse) {
        this.backgroundUniverse = backgroundUniverse;
    }

    public String getFdrMethod() {
        return fdrMethod;
    }

    public void setFdrMethod(String fdrMethod) {
        this.fdrMethod = fdrMethod;
    }
}
