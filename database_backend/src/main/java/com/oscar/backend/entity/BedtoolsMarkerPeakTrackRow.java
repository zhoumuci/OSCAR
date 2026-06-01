package com.oscar.backend.entity;

public class BedtoolsMarkerPeakTrackRow {

    private Long id;
    private String chromosome;
    private Long peakStart;
    private Long peakEnd;
    private String peakName;
    private String groupName;
    private String cellType;
    private String linkedGene;
    private Double log2fc;
    private Double fdr;

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

    public String getPeakName() {
        return peakName;
    }

    public void setPeakName(String peakName) {
        this.peakName = peakName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCellType() {
        return cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getLinkedGene() {
        return linkedGene;
    }

    public void setLinkedGene(String linkedGene) {
        this.linkedGene = linkedGene;
    }

    public Double getLog2fc() {
        return log2fc;
    }

    public void setLog2fc(Double log2fc) {
        this.log2fc = log2fc;
    }

    public Double getFdr() {
        return fdr;
    }

    public void setFdr(Double fdr) {
        this.fdr = fdr;
    }
}
