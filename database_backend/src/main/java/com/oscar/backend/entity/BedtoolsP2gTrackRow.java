package com.oscar.backend.entity;

public class BedtoolsP2gTrackRow {

    private Long id;
    private String chromosome;
    private Long peakStart;
    private Long peakEnd;
    private String targetGene;
    private String linkedPeakRegion;
    private String cellType;
    private String cluster;
    private Double p2gScore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public Long getPeakStart() {
        return peakStart;
    }

    public void setPeakStart(Long peakStart) {
        this.peakStart = peakStart;
    }

    public Long getPeakEnd() {
        return peakEnd;
    }

    public void setPeakEnd(Long peakEnd) {
        this.peakEnd = peakEnd;
    }

    public String getTargetGene() {
        return targetGene;
    }

    public void setTargetGene(String targetGene) {
        this.targetGene = targetGene;
    }

    public String getLinkedPeakRegion() {
        return linkedPeakRegion;
    }

    public void setLinkedPeakRegion(String linkedPeakRegion) {
        this.linkedPeakRegion = linkedPeakRegion;
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

    public Double getP2gScore() {
        return p2gScore;
    }

    public void setP2gScore(Double p2gScore) {
        this.p2gScore = p2gScore;
    }
}
