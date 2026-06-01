package com.oscar.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RegulatoryNetworkLink {

    private String peak;
    private String peakId;
    private String geneSymbol;
    private String linkedGene;
    private Double score;
    private Double linkScore;
    private Double correlation;
    private Double fdr;
    private Double varQAtac;
    private Double varQRna;
    private Long distanceToTss;
    private String group;
    private String cellTypeGroup;
    private String clusterGroup;
    private String markerStatus;
    private String linkType;
    private String source;
    private String datasetId;
    private String sampleName;
    private String domain;
    private Long linkedPeaksCount;
    private Long linkedGenesCount;
    private Long totalLinks;
    private Integer graphLimit;
    private Boolean hasMoreLinks;

    @JsonIgnore
    private String provenanceSource;

    @JsonIgnore
    private String tfName;

    public String getPeak() {
        return peak;
    }

    public void setPeak(String peak) {
        this.peak = peak;
    }

    public String getPeakId() {
        return peakId;
    }

    public void setPeakId(String peakId) {
        this.peakId = peakId;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public String getLinkedGene() {
        return linkedGene;
    }

    public void setLinkedGene(String linkedGene) {
        this.linkedGene = linkedGene;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getLinkScore() {
        return linkScore;
    }

    public void setLinkScore(Double linkScore) {
        this.linkScore = linkScore;
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

    public Long getDistanceToTss() {
        return distanceToTss;
    }

    public void setDistanceToTss(Long distanceToTss) {
        this.distanceToTss = distanceToTss;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getCellTypeGroup() {
        return cellTypeGroup;
    }

    public void setCellTypeGroup(String cellTypeGroup) {
        this.cellTypeGroup = cellTypeGroup;
    }

    public String getClusterGroup() {
        return clusterGroup;
    }

    public void setClusterGroup(String clusterGroup) {
        this.clusterGroup = clusterGroup;
    }

    public String getMarkerStatus() {
        return markerStatus;
    }

    public void setMarkerStatus(String markerStatus) {
        this.markerStatus = markerStatus;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getLinkedPeaksCount() {
        return linkedPeaksCount;
    }

    public void setLinkedPeaksCount(Long linkedPeaksCount) {
        this.linkedPeaksCount = linkedPeaksCount;
    }

    public Long getLinkedGenesCount() {
        return linkedGenesCount;
    }

    public void setLinkedGenesCount(Long linkedGenesCount) {
        this.linkedGenesCount = linkedGenesCount;
    }

    public Long getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(Long totalLinks) {
        this.totalLinks = totalLinks;
    }

    public Integer getGraphLimit() {
        return graphLimit;
    }

    public void setGraphLimit(Integer graphLimit) {
        this.graphLimit = graphLimit;
    }

    public Boolean getHasMoreLinks() {
        return hasMoreLinks;
    }

    public void setHasMoreLinks(Boolean hasMoreLinks) {
        this.hasMoreLinks = hasMoreLinks;
    }

    public String getProvenanceSource() {
        return provenanceSource;
    }

    public void setProvenanceSource(String provenanceSource) {
        this.provenanceSource = provenanceSource;
    }

    public String getTfName() {
        return tfName;
    }

    public void setTfName(String tfName) {
        this.tfName = tfName;
    }
}
