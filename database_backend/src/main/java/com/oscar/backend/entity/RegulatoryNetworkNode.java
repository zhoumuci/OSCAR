package com.oscar.backend.entity;

import java.util.List;

public class RegulatoryNetworkNode {

    private String id;
    private String type;
    private String label;
    private String markerStatus;
    private String group;
    private String chromosome;
    private Long start;
    private Long end;
    private Double markerScore;
    private Integer markerRank;
    private Long cellCount;
    private Double proportion;
    private Double fdr;
    private Double varQAtac;
    private Double varQRna;
    private String datasetId;
    private String sampleName;
    private String domain;
    private Long linkedGenesCount;
    private List<String> topLinkedGenes;
    private Long remainingLinkedGenesCount;
    private Long linkedPeaksCount;
    private List<String> topLinkedPeaks;
    private Long remainingLinkedPeaksCount;
    private Double maxLinkScore;
    private RegulatoryNetworkRange correlationRange;
    private Double minFdr;
    private RegulatoryNetworkRange fdrRange;
    private RegulatoryNetworkRange distanceRange;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMarkerStatus() {
        return markerStatus;
    }

    public void setMarkerStatus(String markerStatus) {
        this.markerStatus = markerStatus;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Double getMarkerScore() {
        return markerScore;
    }

    public void setMarkerScore(Double markerScore) {
        this.markerScore = markerScore;
    }

    public Integer getMarkerRank() {
        return markerRank;
    }

    public void setMarkerRank(Integer markerRank) {
        this.markerRank = markerRank;
    }

    public Long getCellCount() {
        return cellCount;
    }

    public void setCellCount(Long cellCount) {
        this.cellCount = cellCount;
    }

    public Double getProportion() {
        return proportion;
    }

    public void setProportion(Double proportion) {
        this.proportion = proportion;
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

    public Long getLinkedGenesCount() {
        return linkedGenesCount;
    }

    public void setLinkedGenesCount(Long linkedGenesCount) {
        this.linkedGenesCount = linkedGenesCount;
    }

    public List<String> getTopLinkedGenes() {
        return topLinkedGenes;
    }

    public void setTopLinkedGenes(List<String> topLinkedGenes) {
        this.topLinkedGenes = topLinkedGenes;
    }

    public Long getRemainingLinkedGenesCount() {
        return remainingLinkedGenesCount;
    }

    public void setRemainingLinkedGenesCount(Long remainingLinkedGenesCount) {
        this.remainingLinkedGenesCount = remainingLinkedGenesCount;
    }

    public Long getLinkedPeaksCount() {
        return linkedPeaksCount;
    }

    public void setLinkedPeaksCount(Long linkedPeaksCount) {
        this.linkedPeaksCount = linkedPeaksCount;
    }

    public List<String> getTopLinkedPeaks() {
        return topLinkedPeaks;
    }

    public void setTopLinkedPeaks(List<String> topLinkedPeaks) {
        this.topLinkedPeaks = topLinkedPeaks;
    }

    public Long getRemainingLinkedPeaksCount() {
        return remainingLinkedPeaksCount;
    }

    public void setRemainingLinkedPeaksCount(Long remainingLinkedPeaksCount) {
        this.remainingLinkedPeaksCount = remainingLinkedPeaksCount;
    }

    public Double getMaxLinkScore() {
        return maxLinkScore;
    }

    public void setMaxLinkScore(Double maxLinkScore) {
        this.maxLinkScore = maxLinkScore;
    }

    public RegulatoryNetworkRange getCorrelationRange() {
        return correlationRange;
    }

    public void setCorrelationRange(RegulatoryNetworkRange correlationRange) {
        this.correlationRange = correlationRange;
    }

    public Double getMinFdr() {
        return minFdr;
    }

    public void setMinFdr(Double minFdr) {
        this.minFdr = minFdr;
    }

    public RegulatoryNetworkRange getFdrRange() {
        return fdrRange;
    }

    public void setFdrRange(RegulatoryNetworkRange fdrRange) {
        this.fdrRange = fdrRange;
    }

    public RegulatoryNetworkRange getDistanceRange() {
        return distanceRange;
    }

    public void setDistanceRange(RegulatoryNetworkRange distanceRange) {
        this.distanceRange = distanceRange;
    }
}
