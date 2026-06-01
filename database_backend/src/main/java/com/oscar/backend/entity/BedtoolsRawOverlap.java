package com.oscar.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class BedtoolsRawOverlap {

    private String annotationType;
    private String scope;
    private String featureId;
    private String featureChrom;
    private Long featureStart;
    private Long featureEnd;
    private Long overlapBp;
    private Double overlapRatioQuery;
    private Double overlapRatioFeature;
    private List<String> featureColumns = new ArrayList<>();

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
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

    public String getFeatureChrom() {
        return featureChrom;
    }

    public void setFeatureChrom(String featureChrom) {
        this.featureChrom = featureChrom;
    }

    public Long getFeatureStart() {
        return featureStart;
    }

    public void setFeatureStart(Long featureStart) {
        this.featureStart = featureStart;
    }

    public Long getFeatureEnd() {
        return featureEnd;
    }

    public void setFeatureEnd(Long featureEnd) {
        this.featureEnd = featureEnd;
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

    public List<String> getFeatureColumns() {
        return featureColumns;
    }

    public void setFeatureColumns(List<String> featureColumns) {
        this.featureColumns = featureColumns;
    }
}
