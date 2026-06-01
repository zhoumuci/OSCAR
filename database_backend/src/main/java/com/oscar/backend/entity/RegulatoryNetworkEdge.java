package com.oscar.backend.entity;

public class RegulatoryNetworkEdge {

    private String source;
    private String target;
    private String type;
    private Double score;
    private Long distanceToTss;
    private Double correlation;
    private Double fdr;
    private Double varQAtac;
    private Double varQRna;
    private String sourceMethod;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Long getDistanceToTss() {
        return distanceToTss;
    }

    public void setDistanceToTss(Long distanceToTss) {
        this.distanceToTss = distanceToTss;
    }

    public Double getCorrelation() {
        return correlation;
    }

    public void setCorrelation(Double correlation) {
        this.correlation = correlation;
    }

    public Double getFdr() {
        return fdr;
    }

    public void setFdr(Double fdr) {
        this.fdr = fdr;
    }

    public Double getVarQAtac() {
        return varQAtac;
    }

    public void setVarQAtac(Double varQAtac) {
        this.varQAtac = varQAtac;
    }

    public Double getVarQRna() {
        return varQRna;
    }

    public void setVarQRna(Double varQRna) {
        this.varQRna = varQRna;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public void setSourceMethod(String sourceMethod) {
        this.sourceMethod = sourceMethod;
    }
}
