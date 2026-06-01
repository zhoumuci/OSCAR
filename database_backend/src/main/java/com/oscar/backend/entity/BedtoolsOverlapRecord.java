package com.oscar.backend.entity;

import java.util.List;

public class BedtoolsOverlapRecord {

    private String annotationType;
    private String annotationLabel;
    private String scope;
    private String featureId;
    private String featureRegion;
    private Long overlapBp;
    private Double overlapRatioQuery;
    private Double overlapRatioFeature;
    private String gene;
    private String transcriptId;
    private String strand;
    private String cellType;
    private String cluster;
    private String cellCluster;
    private Double score;
    private String evidence;
    private String sample;
    private String name;
    private List<String> rawFields;

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }

    public String getAnnotationLabel() {
        return annotationLabel;
    }

    public void setAnnotationLabel(String annotationLabel) {
        this.annotationLabel = annotationLabel;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getFeatureRegion() {
        return featureRegion;
    }

    public void setFeatureRegion(String featureRegion) {
        this.featureRegion = featureRegion;
    }

    public Long getOverlapBp() {
        return overlapBp;
    }

    public void setOverlapBp(Long overlapBp) {
        this.overlapBp = overlapBp;
    }

    public Double getOverlapRatioQuery() {
        return overlapRatioQuery;
    }

    public void setOverlapRatioQuery(Double overlapRatioQuery) {
        this.overlapRatioQuery = overlapRatioQuery;
    }

    public Double getOverlapRatioFeature() {
        return overlapRatioFeature;
    }

    public void setOverlapRatioFeature(Double overlapRatioFeature) {
        this.overlapRatioFeature = overlapRatioFeature;
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public String getTranscriptId() {
        return transcriptId;
    }

    public void setTranscriptId(String transcriptId) {
        this.transcriptId = transcriptId;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public String getCellType() {
        return cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getCellCluster() {
        return cellCluster;
    }

    public void setCellCluster(String cellCluster) {
        this.cellCluster = cellCluster;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRawFields() {
        return rawFields;
    }

    public void setRawFields(List<String> rawFields) {
        this.rawFields = rawFields;
    }
}
