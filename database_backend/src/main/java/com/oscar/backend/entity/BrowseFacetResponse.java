package com.oscar.backend.entity;

import java.util.List;

public class BrowseFacetResponse {

    private List<BrowseFacetItemResponse> species;
    private List<BrowseFacetItemResponse> sampleType;
    private List<BrowseFacetItemResponse> tissue;

    public BrowseFacetResponse() {
    }

    public BrowseFacetResponse(
            List<BrowseFacetItemResponse> species,
            List<BrowseFacetItemResponse> sampleType,
            List<BrowseFacetItemResponse> tissue
    ) {
        this.species = species;
        this.sampleType = sampleType;
        this.tissue = tissue;
    }

    public List<BrowseFacetItemResponse> getSpecies() {
        return species;
    }

    public void setSpecies(List<BrowseFacetItemResponse> species) {
        this.species = species;
    }

    public List<BrowseFacetItemResponse> getSampleType() {
        return sampleType;
    }

    public void setSampleType(List<BrowseFacetItemResponse> sampleType) {
        this.sampleType = sampleType;
    }

    public List<BrowseFacetItemResponse> getTissue() {
        return tissue;
    }

    public void setTissue(List<BrowseFacetItemResponse> tissue) {
        this.tissue = tissue;
    }
}
